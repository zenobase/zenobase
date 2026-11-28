#!/bin/sh -e
if [ ! -f conf/application-local.yaml ]; then
	echo "conf/application-local.yaml is missing: copy it from the main checkout or conf/application-local.yaml.template" >&2
	exit 1
fi
./mvnw package -DskipTests
docker compose up --build -d
