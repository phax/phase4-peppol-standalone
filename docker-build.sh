#!/bin/sh
docker build --build-arg JAR_FILE=target/*.jar -t phelger/phase4-peppol-standalone .

