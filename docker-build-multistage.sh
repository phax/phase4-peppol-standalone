#!/bin sh

# Multi-stage Docker build script
# This builds the Java application inside Docker, so you don't need local Java/Maven

echo Building phase4-peppol-standalone with multi-stage Docker build...
docker build -f Dockerfile.multistage -t phelger/phase4-peppol-standalone .
echo Build complete!

# To run the resulting image:
# docker run -d -p 8080:8080 phelger/phase4-peppol-standalone
# Access the application at http://localhost:8080
docker run -p 8080:8080 phelger/phase4-peppol-standalone
echo Application is running at http://localhost:8080
