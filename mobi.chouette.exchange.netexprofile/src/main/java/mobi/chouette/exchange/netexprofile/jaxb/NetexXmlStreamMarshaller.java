package mobi.chouette.exchange.netexprofile.jaxb;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.netexprofile.parser.xml.PredefinedSchemaListClasspathResourceResolver;
import org.rutebanken.netex.model.DataManagedObjectStructure;
import org.rutebanken.netex.model.ObjectFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.TIMETABLE_FRAME;

@Log4j
public class NetexXmlStreamMarshaller {

    private static NetexXmlStreamMarshaller instance = null;

    private static JAXBContext context = null;
    private static SchemaFactory schemaFactory = null;
    private static Schema netexSchema = null;
    private static ObjectFactory objectFactory = null;

    private JAXBIntrospector introspector;

    public static NetexXmlStreamMarshaller getInstance() throws Exception {
        if (instance == null) {
            instance = new NetexXmlStreamMarshaller();
        }
        return instance;
    }

    private NetexXmlStreamMarshaller() throws JAXBException, SAXException, URISyntaxException, IOException {
        try {
            if (context == null) {
                context = JAXBContext.newInstance("org.rutebanken.netex.model");
            }
            if (netexSchema == null) {
                Source schemaSource = new StreamSource(getClass().getResourceAsStream("/NeTEx-XML-1.04beta/schema/xsd/NeTEx_publication.xsd"));
                netexSchema = createSchema(schemaSource);
            }
            if (objectFactory == null) {
                objectFactory = new ObjectFactory();
            }
            introspector = context.createJAXBIntrospector();
        } catch (JAXBException e) {
            log.error("Could not initialize JAXB context", e);
            throw new IllegalStateException("Could not initialize JAXB context", e);
        }
    }

    private Marshaller createMarshaller() throws JAXBException, SAXException, FileNotFoundException, MalformedURLException {
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        //marshaller.setEventHandler(new NetexValidationEventHandler());
        //marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NetexNamespacePrefixMapper());
        //marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, schemaLocation);
        //marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, noNamespaceSchemaLocation);

        if (netexSchema != null) {
            //marshaller.setSchema(netexSchema);
            //marshaller.setEventHandler(event -> event.getSeverity() == ValidationEvent.WARNING);
        }
        return marshaller;
    }

    private Schema createSchema(Source source) throws SAXException, IOException {
        if (schemaFactory == null) {
            schemaFactory = createSchemaFactory();
        }
        return schemaFactory.newSchema(source);
    }

    public static SchemaFactory createSchemaFactory() throws IOException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setResourceResolver(new PredefinedSchemaListClasspathResourceResolver("/netex_schema_list.txt"));
        return factory;
    }

    public void marshal(JAXBElement<? extends DataManagedObjectStructure> element, XMLStreamWriter writer) throws JAXBException, FileNotFoundException, SAXException, MalformedURLException {
        if (element == null) {
            throw new IllegalArgumentException("Cannot marshall a NULL object");
        }

        // wrap in custom XMLStreamWriter to workaround JAXB bug: http://java.net/jira/browse/JAXB-614
        //IndentingXMLStreamWriter writer = new IndentingXMLStreamWriter(new EscapingXMLStreamWriter(xmlStreamWriter)); // TODO find out if we need filtering
        //IndentingXMLStreamWriter writer = new IndentingXMLStreamWriter(xmlStreamWriter);

        try {
            // must create a new instance of marshaller as its not thread safe
            Marshaller marshaller = createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.marshal(element, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> Method getJaxbElementFactoryMethod(Class<T> type) {
        for (Method m : objectFactory.getClass().getMethods()) {
            XmlElementDecl xmlElementDecl = m.getAnnotation(XmlElementDecl.class);
            if (xmlElementDecl == null) {
                continue;
            }
            Class<?>[] parameters = m.getParameterTypes();
            if (parameters.length == 1 && parameters[0].isAssignableFrom(type)) {
                return m;
            }
        }
        return null;
    }

    private class NetexNamespacePrefixMapper extends NamespacePrefixMapper {
        private static final String NETEX_PREFIX = ""; // DEFAULT NAMESPACE
        private static final String NETEX_URI = "http://www.netex.org.uk/netex";

        private static final String GML_PREFIX = "gml";
        private static final String GML_URI = "http://www.opengis.net/gml/3.2";

        private static final String SIRI_PREFIX = "siri";
        private static final String SIRI_URI = "http://www.siri.org.uk/siri";

        @Override
        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
            if (NETEX_URI.equals(namespaceUri)) {
                return NETEX_PREFIX;
            } else if (GML_URI.equals(namespaceUri)) {
                return GML_PREFIX;
            } else if (SIRI_URI.equals(namespaceUri)) {
                return SIRI_PREFIX;
            }
            return suggestion;
        }
    }

    private class NetexValidationEventHandler implements ValidationEventHandler {

        @Override
        public boolean handleEvent(ValidationEvent event) {
            switch (event.getSeverity()) {
                case ValidationEvent.FATAL_ERROR:
                    return false;
                case ValidationEvent.ERROR:
                case ValidationEvent.WARNING:
                    log.warn(event.getMessage());
                    break;
            }
            return false;
        }
    }

}
