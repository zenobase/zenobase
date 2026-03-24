#!/bin/sh
./mvnw package -DskipTests
docker compose up --build -d
