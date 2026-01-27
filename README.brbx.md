# Phase4 PEPPOL Standalone Setup

This repository contains a containerized setup for running a PEPPOL Access Point using phase4 and phoss-SMP, with Traefik as a reverse proxy handling SSL termination.

## Prerequisites

- Docker and Docker Compose
- A domain name with the ability to configure DNS records

## Directory Structure

```
.
├── .env              # Environment variables (URLs and hostnames) - create from .env.example
├── .env.example      # Template for environment variables
├── ap-config/         # Phase4 AP configuration
├── ap-generated/      # Generated files for phase4
├── smp-config/        # SMP configuration
├── smp-persistent/    # SMP persistent data
├── traefik/          # Traefik configuration
│   ├── acme.json     # Let's Encrypt certificates
│   ├── config/       # Additional Traefik configuration
│   └── traefik.yml   # Main Traefik configuration
└── docker-compose.yml # Main compose file
```

## Setup Instructions

### 1. Initial Setup

1. Clone this repository:
   ```bash
   git clone <repository-url>
   cd phase4-peppol-standalone
   ```

2. Create required directories and set permissions:
   ```bash
   mkdir -p traefik ap-config ap-generated smp-config smp-persistent
   touch traefik/acme.json
   chmod 600 traefik/acme.json
   ```

3. Copy the environment configuration template:
   ```bash
   cp .env.example .env
   ```
   Then edit `.env` and update the domain URLs (see Environment Configuration section below)

### 2. Configuration

#### Traefik Setup

1. Copy the example Traefik configuration:
   ```bash
   cp traefik-example.yml traefik/traefik.yml
   ```

2. Edit `traefik/traefik.yml` and update:
   - The email address for Let's Encrypt notifications
   - Dashboard security settings if needed
   - Any custom entrypoints or middleware

#### DNS Configuration

Configure your domain's DNS records to point to your server:
- `ap.your-domain.com` → Your server's IP
- `smp.your-domain.com` → Your server's IP

#### Environment Configuration

Copy the example environment file and update it with your domain URLs:

```bash
cp .env.example .env
```

Then edit `.env` and replace the placeholder values with your actual domain names.
Docker Compose automatically reads the `.env` file and uses these variables in `docker-compose.yml`.

### 3. Phase4 Configuration

1. Copy the phase4 configuration file before build to `src/main/resources/application.properties`, afterwards build the docker image with `docker compose build`

2. Copy your PEPPOL Access Point certificate:
   ```bash
   cp /path/to/your/cert-ap.jks ap-config/cert-ap.jks
   chmod 600 ap-config/cert-ap.jks
   ```

3. Configure the following essential properties:
   - PEPPOL participant information
   - Certificate details (point to `cert-ap.jks` in your configuration)
   - Network settings
   - Storage locations
   - Recommand API settings:
     ```properties
     # Internal token for document receiving endpoint
     recommand.api.internal.token=your-token-here
     
     # Recommand API base endpoint (without the API path)
     recommand.api.endpoint=https://peppol.recommand.be
     ```

### 4. SMP Configuration

1. Set up the SMP configuration in `smp-config/`

2. Copy your PEPPOL SMP certificate:
   ```bash
   cp /path/to/your/cert-smp.jks smp-config/cert-smp.jks
   chmod 600 smp-config/cert-smp.jks
   ```

3. Configure:
   - PEPPOL SMP credentials (point to `cert-smp.jks` in your configuration)
   - Access control

### 5. Starting the Services

1. Build and start all services:
   ```bash
   docker compose up -d
   ```

2. Monitor the logs:
   ```bash
   docker compose logs -f
   ```

### 6. Verification

1. Check if Traefik is running:
   ```bash
   curl -I https://ap.your-domain.com
   curl -I https://smp.your-domain.com
   ```

2. Verify SSL certificates are properly issued
3. Test PEPPOL message sending/receiving

## Maintenance

### Updating

To update the services:
```bash
docker compose pull
docker compose build --no-cache phase4
docker compose up -d
```

### Backup

Regular backups should be made of:
- `.env` file (contains your domain configuration)
- `smp-persistent/` directory
- `ap-generated/` directory
- Configuration files in `ap-config/` and `smp-config/`
- Configuration file in`src/main/resources/application.properties`
- Certificate files (`cert-ap.jks` and `cert-smp.jks`)
- Traefik's `acme.json` for SSL certificates