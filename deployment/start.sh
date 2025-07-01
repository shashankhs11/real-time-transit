#!/bin/bash

# Real-Time Transit System Startup Script
# Usage: ./start.sh [local|cloud|prod] [up|down|logs|status]

set -e

ENVIRONMENT=${1:-local}
ACTION=${2:-up}

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_usage() {
    echo -e "${BLUE}Real-Time Transit System Startup Script${NC}"
    echo
    echo "Usage: $0 [ENVIRONMENT] [ACTION]"
    echo
    echo "ENVIRONMENTS:"
    echo "  local  - Local development with containerized Kafka (default)"
    echo "  cloud  - Using Confluent Cloud Kafka"
    echo "  prod   - Full production deployment"
    echo
    echo "ACTIONS:"
    echo "  up     - Start services (default)"
    echo "  down   - Stop services"
    echo "  logs   - Show logs"
    echo "  status - Show service status"
    echo "  build  - Build services"
    echo
    echo "Examples:"
    echo "  $0 local up      # Start local development environment"
    echo "  $0 cloud logs    # Show logs for cloud environment" 
    echo "  $0 prod down     # Stop production environment"
}

check_env_file() {
    if [[ ! -f ../.env ]]; then
        echo -e "${RED}Error: .env file not found in project root${NC}"
        echo "Please create a .env file in the project root with required environment variables"
        echo "See ../docs/DEPLOYMENT.md for details"
        exit 1
    fi
}

validate_environment() {
    case $ENVIRONMENT in
        local|cloud|prod)
            ;;
        help|--help|-h)
            print_usage
            exit 0
            ;;
        *)
            echo -e "${RED}Error: Invalid environment '$ENVIRONMENT'${NC}"
            print_usage
            exit 1
            ;;
    esac
}

get_compose_files() {
    case $ENVIRONMENT in
        local)
            echo "-f docker-compose.yml -f docker-compose.local.yml"
            ;;
        cloud)
            echo "-f docker-compose.base.yml -f docker-compose.cloud.yml"
            ;;
        prod)
            echo "-f docker-compose.prod.yml"
            ;;
    esac
}

execute_action() {
    local compose_files=$(get_compose_files)
    
    case $ACTION in
        up)
            echo -e "${GREEN}Starting $ENVIRONMENT environment...${NC}"
            if [[ $ENVIRONMENT == "local" ]]; then
                echo -e "${YELLOW}This will start Kafka UI at http://localhost:8080${NC}"
            fi
            docker compose --env-file ../.env $compose_files up -d
            echo -e "${GREEN}Services started successfully!${NC}"
            echo
            echo -e "${BLUE}Service URLs:${NC}"
            echo "  Data Ingestion Service: http://localhost:8081"
            echo "  Transit Tracker Service: http://localhost:8082"
            if [[ $ENVIRONMENT == "local" ]]; then
                echo "  Kafka UI: http://localhost:8080"
            fi
            ;;
        down)
            echo -e "${YELLOW}Stopping $ENVIRONMENT environment...${NC}"
            docker compose --env-file ../.env $compose_files down
            echo -e "${GREEN}Services stopped successfully!${NC}"
            ;;
        logs)
            echo -e "${BLUE}Showing logs for $ENVIRONMENT environment...${NC}"
            docker compose --env-file ../.env $compose_files logs -f
            ;;
        status)
            echo -e "${BLUE}Service status for $ENVIRONMENT environment:${NC}"
            docker compose --env-file ../.env $compose_files ps
            ;;
        build)
            echo -e "${YELLOW}Building services for $ENVIRONMENT environment...${NC}"
            docker compose --env-file ../.env $compose_files build
            echo -e "${GREEN}Build completed successfully!${NC}"
            ;;
        *)
            echo -e "${RED}Error: Invalid action '$ACTION'${NC}"
            print_usage
            exit 1
            ;;
    esac
}

# Main execution
main() {
    validate_environment
    
    if [[ $ENVIRONMENT != "help" ]]; then
        check_env_file
    fi
    
    execute_action
}

main "$@"