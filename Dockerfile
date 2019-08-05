FROM jboss/wildfly:8.2.1.Final

USER jboss

RUN mkdir /opt/jboss/wildfly/customization/

COPY docker/files/postgresql-42.2.6.jar /opt/jboss/wildfly/customization/postgresql-42.2.6.jar
COPY docker/files/postgis-jdbc-2.1.7.2.jar /opt/jboss/wildfly/customization/postgis-jdbc-2.1.7.2.jar
COPY docker/files/hibernate-spatial-4.3.jar /opt/jboss/wildfly/modules/system/layers/base/org/hibernate/main/hibernate-spatial-4.3.jar
COPY docker/files/jts-1.13.jar /opt/jboss/wildfly/modules/system/layers/base/org/hibernate/main/jts-1.13.jar

# File where sed expression has been performed:
COPY docker/files/wildfly/module.xml /opt/jboss/wildfly/modules/system/layers/base/org/hibernate/main/module.xml

# Updated JAXB implementation to work with Netex jaxb classes
COPY docker/files/jaxb-impl-2.2.11.jar /opt/jboss/wildfly/modules/system/layers/base/com/sun/xml/bind/main/jaxb-impl-2.2.11.jar
COPY docker/files/jaxb-core-2.2.11.jar /opt/jboss/wildfly/modules/system/layers/base/com/sun/xml/bind/main/jaxb-core-2.2.11.jar
COPY docker/files/jaxb-xjc-2.2.11.jar /opt/jboss/wildfly/modules/system/layers/base/com/sun/xml/bind/main/jaxb-xjc-2.2.11.jar
COPY docker/files/wildfly/jaxb_module.xml /opt/jboss/wildfly/modules/system/layers/base/com/sun/xml/bind/main/module.xml

COPY docker/files/xercesImpl-2.11.0.SP6-RB.jar /opt/jboss/wildfly/modules/system/layers/base/org/apache/xerces/main/xercesImpl-2.11.0.SP6-RB.jar
COPY docker/files/wildfly/xerces_module.xml /opt/jboss/wildfly/modules/system/layers/base/org/apache/xerces/main/module.xml

RUN touch /opt/jboss/wildfly/build.log
RUN chmod a+w /opt/jboss/wildfly/build.log




# Wildfly container configurations, copy and execute
COPY docker/files/wildfly/wildfly_db.cli /tmp/


# Overriding previously installed java version:
RUN curl -L http://static.okina.fr/jdk-8u144-linux-x64.tar.gz > jdk.tgz
RUN tar xzf jdk.tgz
ENV JAVA_HOME /opt/jboss/jdk1.8.0_144/

# Deploying by copying to deployment directory
COPY chouette_iev/target/chouette.ear /tmp/chouette.ear

# Copy standalone customizations
COPY docker/files/wildfly/standalone.conf /opt/jboss/wildfly/bin
# From http://stackoverflow.com/questions/20965737/docker-jboss7-war-commit-server-boot-failed-in-an-unrecoverable-manner
RUN rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history \
  && mkdir -p /opt/jboss/data \
  && chown jboss:jboss /opt/jboss/data

# Running as root, in order to get mounted volume writable:
USER root

RUN /opt/jboss/wildfly/bin/add-user.sh admin password --silent

COPY docker/docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]

CMD [ "/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
