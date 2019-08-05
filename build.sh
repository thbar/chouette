#!/bin/bash
MVN_VERSION=$(mvn -q \
    -Dexec.executable=echo \
    -Dexec.args='${project.version}' \
    --non-recursive \
    exec:exec)

#mvn  install -DskipTests -DskipWildfly=true -DskipInitDb=true
docker build -t registry.okina.fr/mosaic/chouette:$MVN_VERSION .
docker push registry.okina.fr/mosaic/chouette:$MVN_VERSION