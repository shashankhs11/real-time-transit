#!/bin/bash

# Real-Time Transit Tracker - Quick Setup Script
set -e

echo "Real-Time Transit Tracker - Quick Setup"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is installed
check_docker() {
    print_status "Checking Docker installation..."
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        echo "Visit: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        echo "Visit: https://docs.docker.com/compose/install/"
        exit 1
    fi
    
    print_success "Docker and Docker Compose are installed"
}

# Download GTFS data
download_gtfs() {
    print_status "Downloading GTFS data from TransLink..."
    if [ ! -f "google_transit.zip" ]; then
        curl -o google_transit.zip "https://gtfs.translink.ca/gtfs/google_transit.zip"
        if [ $? -eq 0 ]; then
            print_success "GTFS data downloaded successfully"
        else
            print_error "Failed to download GTFS data"
            exit 1
        fi
    else
        print_warning "GTFS data already exists. Use --force to re-download."
    fi
}

# Setup environment file
setup_env() {
    print_status "Setting up environment configuration..."
    if [ ! -f ".env" ]; then
        cp .env.template .env
        print_warning "Environment file created from template"
        print_warning "Please edit .env file and add your TRANSLINK_API_KEY"
        echo ""
        echo "You can get a TransLink API key from:"
        echo "https://developer.translink.ca/"
        echo ""
        read -p "Press Enter after you've updated the .env file..."
    else
        print_success "Environment file already exists"
    fi
}

# Build and start services
start_services() {
    print_status "Building and starting services..."
    print_status "This may take several minutes on first run..."
    
    docker-compose up --build -d
    
    if [ $? -eq 0 ]; then
        print_success "Services started successfully"
    else
        print_error "Failed to start services"
        exit 1
    fi
}

# Wait for services to be healthy
wait_for_services() {
    print_status "Waiting for services to be healthy..."
    
    max_attempts=30
    attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        print_status "Checking service health (attempt $attempt/$max_attempts)..."
        
        # Check data ingestion service
        if curl -sf http://localhost:8081/actuator/health > /dev/null 2>&1; then
            data_ingestion_healthy=true
        else
            data_ingestion_healthy=false
        fi
        
        # Check transit tracker service
        if curl -sf http://localhost:8082/actuator/health > /dev/null 2>&1; then
            transit_tracker_healthy=true
        else
            transit_tracker_healthy=false
        fi
        
        if [ "$data_ingestion_healthy" = true ] && [ "$transit_tracker_healthy" = true ]; then
            print_success "All services are healthy!"
            break
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            print_error "Services did not become healthy within expected time"
            print_status "Check logs with: docker-compose logs"
            exit 1
        fi
        
        sleep 10
        ((attempt++))
    done
}

# Test API endpoints
test_apis() {
    print_status "Testing API endpoints..."
    
    # Test routes endpoint
    if curl -sf "http://localhost:8082/api/routes" > /dev/null 2>&1; then
        route_count=$(curl -s "http://localhost:8082/api/routes" | jq length 2>/dev/null || echo "unknown")
        print_success "Routes API working (found $route_count routes)"
    else
        print_warning "Routes API not responding"
    fi
    
    # Test search endpoint
    if curl -sf "http://localhost:8082/api/search/routes?q=49" > /dev/null 2>&1; then
        print_success "Search API working"
    else
        print_warning "Search API not responding"
    fi
    
    # Test health endpoints
    print_success "Health endpoints:"
    echo "  - Data Ingestion: http://localhost:8081/actuator/health"
    echo "  - Transit Tracker: http://localhost:8082/actuator/health"
    echo "  - Kafka UI: http://localhost:8080"
}

# Display summary
show_summary() {
    echo ""
    echo "=========================================="
    print_success "Setup completed successfully!"
    echo "=========================================="
    echo ""
    echo "Service URLs:"
    echo "  • Transit Tracker API: http://localhost:8082"
    echo "  • Data Ingestion API: http://localhost:8081"
    echo "  • Kafka UI: http://localhost:8080"
    echo ""
    echo "API Documentation:"
    echo "  • See API_DOCUMENTATION.md for complete API reference"
    echo ""
    echo "Useful Commands:"
    echo "  • View logs: docker-compose logs -f"
    echo "  • Stop services: docker-compose down"
    echo "  • Restart services: docker-compose restart"
    echo ""
    echo "Test Commands:"
    echo "  • List routes: curl http://localhost:8082/api/routes | jq .[0:5]"
    echo "  • Search route 49: curl \"http://localhost:8082/api/search/routes?q=49\" | jq"
    echo "  • Check health: curl http://localhost:8082/actuator/health"
    echo ""
}

# Main execution
main() {
    # Parse command line arguments
    FORCE=false
    while [[ $# -gt 0 ]]; do
        case $1 in
            --force)
                FORCE=true
                shift
                ;;
            --help)
                echo "Usage: $0 [--force] [--help]"
                echo "  --force: Force re-download of GTFS data"
                echo "  --help:  Show this help message"
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    # Force re-download if requested
    if [ "$FORCE" = true ] && [ -f "google_transit.zip" ]; then
        print_status "Forcing re-download of GTFS data..."
        rm google_transit.zip
    fi
    
    # Run setup steps
    check_docker
    download_gtfs
    setup_env
    start_services
    wait_for_services
    test_apis
    show_summary
}

# Run main function
main "$@"