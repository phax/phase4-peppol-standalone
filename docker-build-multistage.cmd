@REM
@REM Copyright (C) 2023-2026 Philip Helger (www.helger.com)
@REM philip[at]helger[dot]com
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM         http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

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
