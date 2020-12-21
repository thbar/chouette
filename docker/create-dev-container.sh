#!/usr/bin/env bash
# Créer le container depuis l'image de base , avec les props nécessaire pour la datasource et la
# surcharge de la commande avec le --debug
docker run \
-p 8080:8080 \
-p 8787:8787 \
-p 9990:9990 \
--name mobiiti-chouette-dev \
--env CHOUETTE_DB_HOST=192.168.24.59 \
--env CHOUETTE_DB_PORT=5449 \
--env CHOUETTE_DB_USER=chouette \
--env CHOUETTE_DB_PASSWORD=chouette \
--env CHOUETTE_DB_NAME=chouette \
--env JAVA_OPTS="-Xms1024m -Xmx2048m -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=512m -Djava.net.preferIPv4Stack=true -Djboss.modules.system.pkgs=org.jboss.byteman -Djava.awt.headless=true" \
registry.okina.fr/mobiiti/chouette-base:1.0 \
/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0 --debug
