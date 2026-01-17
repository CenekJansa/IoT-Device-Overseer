#!/bin/env sh

sudo mvn clean package

cleanup() {
    echo -e "\n--- Running Cleanup (docker compose down --volumes) ---"
    sudo docker compose down --volumes
    exit
}

sudo docker compose up --build

cleanup