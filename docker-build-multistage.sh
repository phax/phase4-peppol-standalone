#!/bin sh
#
# Copyright (C) 2023-2026 Philip Helger (www.helger.com)
# philip[at]helger[dot]com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


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
