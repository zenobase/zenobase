#!/bin/sh -e
./mvnw package -DskipTests
docker compose up --build -d
