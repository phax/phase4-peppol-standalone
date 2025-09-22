@REM
@REM Multi-stage Docker build script
@REM This builds the Java application inside Docker, so you don't need local Java/Maven
@REM

@echo off
echo Building phase4-peppol-standalone with multi-stage Docker build...
docker build -f Dockerfile.multistage -t phelger/phase4-peppol-standalone .
echo Build complete!

@REM To run the resulting image:
@REM docker run -d -p 8080:8080 phelger/phase4-peppol-standalone
@REM Access the application at http://localhost:8080
docker run -p 8080:8080 phelger/phase4-peppol-standalone
echo Application is running at http://localhost:8080
