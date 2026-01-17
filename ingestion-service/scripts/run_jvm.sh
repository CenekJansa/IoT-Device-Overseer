#!/bin/bash

# Fail on error
set -e


# Before building the container image run:
#
./mvnw package
#
# Then, build the image with:
#
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/ingestion-service-jvm .
#
# Then run the container using:
#
docker run -i --rm -p 8082:8082 quarkus/ingestion-service-jvm
#
# If you want to include the debug port into your docker image
# you will have to expose the debug port (default 5005 being the default) like this :  EXPOSE 8082 5005.
# Additionally you will have to set -e JAVA_DEBUG=true and -e JAVA_DEBUG_PORT=*:5005
# when running the container
#
# Then run the container using :
#
# docker run -i --rm -p 8082:8082 quarkus/ingestion-service-jvm