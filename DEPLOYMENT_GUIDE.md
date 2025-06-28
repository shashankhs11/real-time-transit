# Real-Time Transit Tracker - Deployment Guide

This guide provides step-by-step instructions for deploying the Real-Time Transit Tracker application both locally and on Oracle Linux Cloud Infrastructure.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Production Deployment on Oracle Linux](#production-deployment-on-oracle-linux)
4. [Configuration](#configuration)
5. [Monitoring and Maintenance](#monitoring-and-maintenance)
6. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- **Docker** (version 20.10+)
- **Docker Compose** (version 2.0+)
- **Git**
- **TransLink API Key** (get from [TransLink Developer Portal](https://developer.translink.ca/))

### Required Files
- `google_transit.zip` - GTFS static data file from TransLink
- TransLink API key for real-time data access

### System Requirements

#### Local Development
- **Memory**: 8GB RAM minimum, 16GB recommended
- **Storage**: 5GB free space
- **CPU**: 4 cores recommended

#### Production (Oracle Linux Free Tier)
- **Memory**: 24GB RAM (4 OCPUs, Ampere)
- **Storage**: 50GB+ recommended
- **CPU**: 4 OCPUs (sufficient for production load)

---

## Local Development Setup

### Step 1: Clone and Prepare the Repository

```bash
# Clone the repository
git clone <your-repo-url>
cd real-time-transit

# Verify project structure
ls -la
# Should see: data-ingestion-service/, transit-tracker-service/, shared-models/, docker-compose.yml
```

### Step 2: Download GTFS Data

```bash
# Download the latest GTFS data from TransLink
curl -o google_transit.zip "https://gtfs.translink.ca/gtfs/google_transit.zip"

# Verify the file exists and has content
ls -lh google_transit.zip
# Should show a file of several MB in size
```

### Step 3: Configure Environment Variables

```bash
# Copy the environment template
cp .env.template .env

# Edit the .env file with your actual values
nano .env
```

**Required values in `.env`:**
```bash
TRANSLINK_API_KEY=your_actual_translink_api_key_here
```

### Step 4: Build and Start Local Environment

```bash
# Build and start all services
docker-compose up --build -d

# Check that all services are starting
docker-compose ps
```

**Expected output:**
```
NAME                    COMMAND                  SERVICE                   STATUS              PORTS
transit-data-ingestion  "sh -c 'java $JAVA_O…"   data-ingestion-service    Up (healthy)        0.0.0.0:8081->8081/tcp
transit-kafka           "/etc/confluent/dock…"   kafka                     Up (healthy)        0.0.0.0:9092->9092/tcp, 0.0.0.0:9094->9094/tcp
transit-kafka-ui        "/bin/sh -c 'java --…"   kafka-ui                  Up                  0.0.0.0:8080->8080/tcp
transit-tracker         "sh -c 'java $JAVA_O…"   transit-tracker-service   Up (healthy)        0.0.0.0:8082->8082/tcp
transit-zookeeper       "/etc/confluent/dock…"   zookeeper                 Up                  0.0.0.0:2181->2181/tcp
```

### Step 5: Verify Local Deployment

```bash
# Check service health
curl http://localhost:8081/actuator/health  # Data Ingestion Service
curl http://localhost:8082/actuator/health  # Transit Tracker Service

# Test API endpoints
curl http://localhost:8082/api/routes | jq '.[0:3]'  # First 3 routes
curl "http://localhost:8082/api/search/routes?q=49" | jq  # Search for route 49

# Monitor Kafka (optional)
# Open http://localhost:8080 in browser for Kafka UI
```

### Step 6: Monitor Logs

```bash
# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f data-ingestion-service
docker-compose logs -f transit-tracker-service

# Check if data ingestion is working
docker-compose logs data-ingestion-service | grep "Poll #"
```

---

## Production Deployment on Oracle Linux

### Step 1: Prepare Oracle Linux Instance

#### Create Oracle Linux Instance
1. Log into Oracle Cloud Console
2. Create Compute Instance:
   - **Image**: Oracle Linux 9
   - **Shape**: VM.Standard.A1.Flex (4 OCPUs, 24GB RAM)
   - **Network**: Allow HTTP/HTTPS traffic
   - **SSH**: Upload your public key

#### Configure Firewall
```bash
# SSH into your Oracle Linux instance
ssh -i your-key.pem opc@your-instance-ip

# Open required ports
sudo firewall-cmd --permanent --add-port=8081/tcp  # Data Ingestion
sudo firewall-cmd --permanent --add-port=8082/tcp  # Transit Tracker
sudo firewall-cmd --permanent --add-port=80/tcp    # HTTP
sudo firewall-cmd --permanent --add-port=443/tcp   # HTTPS
sudo firewall-cmd --reload

# Verify ports are open
sudo firewall-cmd --list-ports
```

### Step 2: Install Docker and Dependencies

```bash
# Update system
sudo dnf update -y

# Install Docker
sudo dnf install -y docker docker-compose git curl

# Start and enable Docker
sudo systemctl start docker
sudo systemctl enable docker

# Add user to docker group
sudo usermod -aG docker $USER

# Log out and back in, or run:
newgrp docker

# Verify Docker installation
docker --version
docker-compose --version
```

### Step 3: Deploy Application

```bash
# Clone repository
git clone <your-repo-url>
cd real-time-transit

# Download GTFS data
curl -o google_transit.zip "https://gtfs.translink.ca/gtfs/google_transit.zip"

# Create production environment file
cp .env.template .env.prod

# Edit production environment
nano .env.prod
```

**Production `.env.prod` file:**
```bash
# TransLink API Key (required)
TRANSLINK_API_KEY=your_actual_translink_api_key_here

# Kafka Configuration (use Confluent Cloud or other managed Kafka)
KAFKA_BOOTSTRAP_SERVERS=your-kafka-cluster.confluent.cloud:9092
KAFKA_API_KEY=your_kafka_api_key
KAFKA_API_SECRET=your_kafka_api_secret

# Application Configuration
POLLING_ENABLED=true
POLLING_INTERVAL_SECONDS=30

# Java Options (optimized for 24GB RAM)
DATA_INGESTION_JAVA_OPTS=-Xmx4g -Xms2g -server -XX:+UseG1GC
TRANSIT_TRACKER_JAVA_OPTS=-Xmx8g -Xms4g -server -XX:+UseG1GC
```

### Step 4: Start Production Services

```bash
# Load environment variables
set -a
source .env.prod
set +a

# Start production deployment
docker-compose -f docker-compose.prod.yml up --build -d

# Monitor startup
docker-compose -f docker-compose.prod.yml logs -f
```

### Step 5: Configure Nginx (Optional but Recommended)

```bash
# If using the nginx service, ensure SSL certificates are configured
# For Let's Encrypt certificates:
sudo dnf install -y certbot

# Generate SSL certificate (replace with your domain)
sudo certbot certonly --standalone -d your-domain.com

# Copy certificates to nginx directory
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem nginx/ssl/key.pem
sudo chown -R $USER:$USER nginx/ssl/

# Uncomment HTTPS server block in nginx/nginx.conf
# Then restart nginx
docker-compose -f docker-compose.prod.yml restart nginx
```

### Step 6: Setup Auto-renewal and Monitoring

#### SSL Certificate Auto-renewal
```bash
# Add to crontab for automatic SSL renewal
echo "0 12 * * * /usr/bin/certbot renew --quiet" | sudo crontab -
```

#### Log Rotation
```bash
# Configure log rotation for Docker
sudo tee /etc/logrotate.d/docker > /dev/null <<EOF
/var/lib/docker/containers/*/*.log {
  rotate 7
  daily
  compress
  size=1M
  missingok
  delaycompress
  copytruncate
}
EOF
```

#### System Monitoring Script
```bash
# Create monitoring script
cat > monitor.sh << 'EOF'
#!/bin/bash
# Check service health and restart if needed

SERVICES="data-ingestion-service transit-tracker-service"

for service in $SERVICES; do
    if ! docker-compose -f docker-compose.prod.yml ps $service | grep -q "Up"; then
        echo "$(date): $service is down, restarting..."
        docker-compose -f docker-compose.prod.yml restart $service
    fi
done
EOF

chmod +x monitor.sh

# Add to crontab to run every 5 minutes
echo "*/5 * * * * /home/opc/real-time-transit/monitor.sh" | crontab -
```

---

## Configuration

### Kafka Configuration Options

#### Option 1: Confluent Cloud (Recommended for Production)
```bash
KAFKA_BOOTSTRAP_SERVERS=pkc-xxxxx.us-west-2.aws.confluent.cloud:9092
KAFKA_API_KEY=your_confluent_api_key
KAFKA_API_SECRET=your_confluent_api_secret
```

#### Option 2: Self-hosted Kafka
```bash
KAFKA_BOOTSTRAP_SERVERS=your-kafka-server:9092
KAFKA_API_KEY=""  # Leave empty for PLAINTEXT
KAFKA_API_SECRET=""  # Leave empty for PLAINTEXT
```

### Memory Configuration

#### For 24GB Oracle Linux Instance:
```bash
# Conservative settings
DATA_INGESTION_JAVA_OPTS=-Xmx4g -Xms2g -server -XX:+UseG1GC
TRANSIT_TRACKER_JAVA_OPTS=-Xmx8g -Xms4g -server -XX:+UseG1GC

# Aggressive settings (if you have no other services)
DATA_INGESTION_JAVA_OPTS=-Xmx6g -Xms3g -server -XX:+UseG1GC
TRANSIT_TRACKER_JAVA_OPTS=-Xmx12g -Xms6g -server -XX:+UseG1GC
```

### Application Configuration

#### Polling Configuration
```bash
POLLING_ENABLED=true
POLLING_INTERVAL_SECONDS=30  # TransLink API allows updates every 30 seconds
```

#### Logging Configuration
```bash
# Add to environment for debug logging
LOGGING_LEVEL_COM_BUSTRACKER=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_KAFKA=INFO
```

---

## Monitoring and Maintenance

### Health Checks

```bash
# Service health endpoints
curl http://your-server:8081/actuator/health
curl http://your-server:8082/actuator/health

# Check service statistics
curl http://your-server:8081/stats
curl http://your-server:8082/api/routes/6636/vehicles
```

### Log Monitoring

```bash
# View live logs
docker-compose -f docker-compose.prod.yml logs -f --tail=100

# Check for errors
docker-compose -f docker-compose.prod.yml logs | grep ERROR

# Monitor disk usage
df -h
docker system df
```

### Performance Monitoring

```bash
# Container resource usage
docker stats

# System resource usage
htop

# Check memory usage
free -h
```

### Backup and Updates

#### Backup Configuration
```bash
# Backup configuration files
tar -czf backup-$(date +%Y%m%d).tar.gz \
    .env.prod docker-compose.prod.yml nginx/ google_transit.zip
```

#### Update GTFS Data
```bash
# Download latest GTFS data
curl -o google_transit_new.zip "https://gtfs.translink.ca/gtfs/google_transit.zip"

# Replace old file and restart tracker service
mv google_transit.zip google_transit_backup.zip
mv google_transit_new.zip google_transit.zip
docker-compose -f docker-compose.prod.yml restart transit-tracker-service
```

#### Update Application
```bash
# Pull latest code
git pull origin main

# Rebuild and restart services
docker-compose -f docker-compose.prod.yml up --build -d
```

---

## Troubleshooting

### Common Issues

#### Services Won't Start
```bash
# Check logs for errors
docker-compose logs service-name

# Check if ports are available
netstat -tlnp | grep :8081
netstat -tlnp | grep :8082

# Restart services
docker-compose restart service-name
```

#### Out of Memory Errors
```bash
# Check memory usage
free -h
docker stats

# Reduce Java heap sizes in environment variables
DATA_INGESTION_JAVA_OPTS=-Xmx2g -Xms1g -server
TRANSIT_TRACKER_JAVA_OPTS=-Xmx4g -Xms2g -server
```

#### Kafka Connection Issues
```bash
# Test Kafka connectivity
docker exec -it transit-data-ingestion curl -f kafka:29092

# Check Kafka logs
docker-compose logs kafka

# Verify Kafka topics
docker exec -it transit-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

#### API Not Responding
```bash
# Check if services are healthy
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# Check nginx configuration (if using)
docker exec -it transit-nginx nginx -t

# Restart nginx
docker-compose restart nginx
```

#### GTFS Data Loading Issues
```bash
# Check if GTFS file exists and is valid
ls -la google_transit.zip
unzip -t google_transit.zip

# Check tracker service logs during startup
docker-compose logs transit-tracker-service | grep GTFS
```

### Performance Optimization

#### For High Load
```bash
# Increase JVM heap sizes
TRANSIT_TRACKER_JAVA_OPTS=-Xmx12g -Xms6g -server -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# Add JVM garbage collection tuning
-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication
```

#### For Limited Memory
```bash
# Reduce heap sizes
DATA_INGESTION_JAVA_OPTS=-Xmx1g -Xms512m -server
TRANSIT_TRACKER_JAVA_OPTS=-Xmx3g -Xms1g -server

# Disable polling if only API access is needed
POLLING_ENABLED=false
```

### Emergency Recovery

#### Complete Reset
```bash
# Stop all services
docker-compose -f docker-compose.prod.yml down

# Remove all containers and volumes
docker system prune -a --volumes

# Restart from clean state
docker-compose -f docker-compose.prod.yml up --build -d
```

#### Rollback Deployment
```bash
# Revert to previous Git commit
git reset --hard HEAD~1

# Rebuild with previous version
docker-compose -f docker-compose.prod.yml up --build -d
```

---

## Support and Maintenance

### Regular Maintenance Tasks

1. **Weekly**: Check logs for errors and warnings
2. **Monthly**: Update GTFS data, check disk usage
3. **Quarterly**: Update system packages and Docker images
4. **As needed**: Monitor API key usage and renewal

### Monitoring Checklist

- [ ] Services are healthy (`/actuator/health`)
- [ ] API endpoints are responding
- [ ] Kafka messages are being produced/consumed
- [ ] Disk space is sufficient
- [ ] Memory usage is within limits
- [ ] SSL certificates are valid (if using HTTPS)
- [ ] API keys are not expired

This deployment guide provides a complete setup for both local development and production deployment on Oracle Linux. Follow the steps carefully and refer to the troubleshooting section if you encounter any issues.