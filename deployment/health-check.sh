#!/bin/bash

# Health Check Script for Real-Time Transit System
# Usage: ./health-check.sh [--timeout=300] [--verbose]

set -e

# Default configuration
TIMEOUT=300
VERBOSE=false
SERVICES=("transit-tracker" "transit-data-ingestion")

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse command line arguments
for arg in "$@"; do
    case $arg in
        --timeout=*)
            TIMEOUT="${arg#*=}"
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "Usage: $0 [--timeout=300] [--verbose]"
            echo "  --timeout=N   Set timeout in seconds (default: 300)"
            echo "  --verbose     Enable verbose output"
            exit 0
            ;;
    esac
done

log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

verbose_log() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${BLUE}[DEBUG]${NC} $1"
    fi
}

# Function to test endpoint with retries
test_endpoint() {
    local url=$1
    local description=$2
    local expected_status=${3:-200}
    local max_attempts=10
    local attempt=1
    
    log "Testing $description..."
    
    while [ $attempt -le $max_attempts ]; do
        verbose_log "Attempt $attempt/$max_attempts for $url"
        
        if response=$(curl -s -w "%{http_code}" -o /tmp/health_check_response.json "$url" 2>/dev/null); then
            status_code="${response: -3}"
            if [ "$status_code" = "$expected_status" ]; then
                log_success "$description is responding (HTTP $status_code)"
                if [ "$VERBOSE" = true ] && [ -f /tmp/health_check_response.json ]; then
                    echo "Response: $(cat /tmp/health_check_response.json)"
                fi
                return 0
            else
                verbose_log "$description returned HTTP $status_code (expected $expected_status)"
            fi
        else
            verbose_log "curl command failed for $url"
        fi
        
        if [ $attempt -lt $max_attempts ]; then
            verbose_log "Retrying in 5 seconds..."
            sleep 5
        fi
        attempt=$((attempt + 1))
    done
    
    log_error "$description failed after $max_attempts attempts"
    return 1
}

# Function to check container status
check_containers() {
    log "Checking container status..."
    
    for service in "${SERVICES[@]}"; do
        verbose_log "Checking container: $service"
        
        if docker ps --format "table {{.Names}}" | grep -q "^$service$"; then
            local status=$(docker ps --filter "name=$service" --format "{{.Status}}")
            if [[ $status == *"Up"* ]]; then
                log_success "Container $service is running: $status"
            else
                log_error "Container $service is not healthy: $status"
                return 1
            fi
        else
            log_error "Container $service is not running"
            return 1
        fi
    done
}

# Function to check logs for errors
check_logs() {
    log "Checking recent logs for errors..."
    
    for service in "${SERVICES[@]}"; do
        verbose_log "Checking logs for: $service"
        
        # Check for recent errors (last 50 lines)
        if docker logs --tail 50 "$service" 2>&1 | grep -i "error\|exception\|failed" | grep -v "Connection to node -1" > /tmp/${service}_errors.log; then
            if [ -s /tmp/${service}_errors.log ]; then
                log_warning "Found potential errors in $service logs:"
                if [ "$VERBOSE" = true ]; then
                    cat /tmp/${service}_errors.log
                fi
            fi
        fi
        
        # Check if service started successfully
        if docker logs --tail 20 "$service" 2>&1 | grep -q "Started.*Application\|Tomcat started on port"; then
            log_success "$service started successfully"
        else
            log_warning "$service may not have started properly"
        fi
    done
}

# Function to run comprehensive API tests
test_apis() {
    log "Running comprehensive API tests..."
    
    # Health endpoints
    test_endpoint "http://localhost:8082/actuator/health" "Transit Tracker Health"
    test_endpoint "http://localhost:8081/actuator/health" "Data Ingestion Health"
    
    # Core API endpoints
    test_endpoint "http://localhost:8082/api/routes" "Routes API"
    
    # Test feedback API validation (should return 400)
    log "Testing Feedback API validation..."
    if response=$(curl -s -w "%{http_code}" -X POST http://localhost:8082/api/feedback \
        -H "Content-Type: application/json" \
        -d '{"feedback": ""}' -o /tmp/feedback_validation.json 2>/dev/null); then
        status_code="${response: -3}"
        if [ "$status_code" = "400" ]; then
            log_success "Feedback API validation is working correctly"
            verbose_log "Validation response: $(cat /tmp/feedback_validation.json)"
        else
            log_error "Feedback API validation test failed (expected 400, got $status_code)"
            if [ "$VERBOSE" = true ]; then
                echo "Response: $(cat /tmp/feedback_validation.json)"
            fi
            return 1
        fi
    else
        log_error "Failed to test feedback API validation"
        return 1
    fi
    
    # Test valid feedback submission
    log "Testing valid feedback submission..."
    if response=$(curl -s -w "%{http_code}" -X POST http://localhost:8082/api/feedback \
        -H "Content-Type: application/json" \
        -d "{\"feedback\": \"Health check test - $(date)\"}" -o /tmp/feedback_success.json 2>/dev/null); then
        status_code="${response: -3}"
        if [ "$status_code" = "200" ]; then
            log_success "Feedback API is working correctly"
            verbose_log "Success response: $(cat /tmp/feedback_success.json)"
        else
            log_error "Feedback API test failed (expected 200, got $status_code)"
            if [ "$VERBOSE" = true ]; then
                echo "Response: $(cat /tmp/feedback_success.json)"
            fi
            return 1
        fi
    else
        log_error "Failed to test feedback API"
        return 1
    fi
}

# Function to check system resources
check_resources() {
    log "Checking system resources..."
    
    # Check memory usage
    local memory_usage=$(free | awk 'NR==2{printf "%.1f", $3/$2*100}')
    if [ "$(echo "$memory_usage > 90" | bc)" -eq 1 ]; then
        log_warning "High memory usage: ${memory_usage}%"
    else
        log_success "Memory usage is acceptable: ${memory_usage}%"
    fi
    
    # Check disk space
    local disk_usage=$(df . | awk 'NR==2{print $5}' | sed 's/%//')
    if [ "$disk_usage" -gt 90 ]; then
        log_warning "High disk usage: ${disk_usage}%"
    else
        log_success "Disk usage is acceptable: ${disk_usage}%"
    fi
    
    verbose_log "System resources check completed"
}

# Main health check function
main() {
    log "🔍 Starting health check for Real-Time Transit System..."
    log "Timeout: ${TIMEOUT}s, Verbose: $VERBOSE"
    
    # Set timeout for the entire script
    timeout $TIMEOUT bash -c '
        check_containers || exit 1
        check_logs
        sleep 10  # Allow services to fully initialize
        test_apis || exit 1
        check_resources
    ' || {
        log_error "Health check timed out or failed after ${TIMEOUT} seconds"
        exit 1
    }
    
    log_success "🎉 All health checks passed! System is healthy."
    
    # Clean up temporary files
    rm -f /tmp/health_check_response.json /tmp/feedback_*.json /tmp/*_errors.log
    
    return 0
}

# Export functions for timeout subshell
export -f check_containers check_logs test_apis check_resources log log_success log_warning log_error verbose_log test_endpoint
export VERBOSE SERVICES BLUE GREEN YELLOW RED NC

# Run main function
main "$@"