#!/bin/bash

# Version de l'image de base. Décorellé de la version applicative, n'évolue pas souvent.
CHOUETTE_BASE_VERSION=1.1

MVN_VERSION=$(mvn -q \
    -Dexec.executable=echo \
    -Dexec.args='${project.version}' \
    --non-recursive \
    exec:exec)


#mvn  install -DskipTests -DskipWildfly=true -DskipInitDb=true
docker build -t registry.okina.fr/mobiiti/chouette:${MVN_VERSION} -f docker/Dockerfile-build --build-arg CHOUETTE_BASE_VERSION=registry.okina.fr/mobiiti/chouette-base:${CHOUETTE_BASE_VERSION} .
docker push registry.okina.fr/mobiiti/chouette:${MVN_VERSION}
