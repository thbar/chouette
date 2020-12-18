#!/bin/bash

# Version de l'image de base. Décorellé de la version applicative, n'évolue pas souvent.
CHOUETTE_BASE_VERSION=1.0

docker build -t registry.okina.fr/mobiiti/chouette-base:${CHOUETTE_BASE_VERSION} -f docker/Dockerfile-base .
docker push registry.okina.fr/mobiiti/chouette-base:${CHOUETTE_BASE_VERSION}
