/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */
package mobi.chouette.exchange.neptune.jaxb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import lombok.extern.log4j.Log4j;

import mobi.chouette.exchange.neptune.exporter.producer.AbstractJaxbNeptuneProducer;
import org.trident.schema.trident.ChouettePTNetworkType;
import org.xml.sax.SAXException;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * Reader tool to extract XML Neptune Schema Objects (jaxb) from a file or a
 * stream
 */
@Log4j
public class JaxbNeptuneFileConverter {

	private JAXBContext context = null;

	private Schema schema = null;

	private static JaxbNeptuneFileConverter instance = null;

	public static JaxbNeptuneFileConverter getInstance() throws Exception {
		if (instance == null)
			instance = new JaxbNeptuneFileConverter();
		return instance;
	}

	/**
	 * constructor
	 * 
	 * @throws JAXBException
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws IOException
	 */
	private JaxbNeptuneFileConverter() throws JAXBException, SAXException, URISyntaxException, IOException {
		context = JAXBContext.newInstance(ChouettePTNetworkType.class);
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schema = schemaFactory.newSchema(getClass().getClassLoader().getResource("xsd/neptune.xsd"));
	}

	public void write(ChouettePTNetworkType rootObject, File file) throws JAXBException, IOException {
		write(AbstractJaxbNeptuneProducer.tridentFactory.createChouettePTNetwork(rootObject), new FileOutputStream(file));
	}

	public void write(JAXBElement<ChouettePTNetworkType> rootObject, File file) throws JAXBException, IOException {
		write(rootObject, new FileOutputStream(file));
	}

	public void write(JAXBElement<ChouettePTNetworkType> network, OutputStream stream) throws JAXBException,
			IOException {
		try {
			Marshaller marshaller = context.createMarshaller();
			marshaller.setSchema(schema);
			marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8"); // NOI18N
			marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setEventHandler(new NeptuneValidationEventHandler());
			NamespacePrefixMapper mapper = new NeptuneNamespacePrefixMapper();
			//marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", mapper);
			marshaller.marshal(network, stream);
		} finally {
			stream.close();
		}
	}


	private class NeptuneValidationEventHandler implements ValidationEventHandler {

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


	public Optional<ChouettePTNetworkType> read(Path path){
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(org.trident.schema.trident.ObjectFactory.class);

			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			JAXBElement<ChouettePTNetworkType> chouetteRoute = (JAXBElement<ChouettePTNetworkType>) unmarshaller.unmarshal(new File(path.toAbsolutePath().toString()));
			return Optional.of(chouetteRoute.getValue());

		} catch (JAXBException e) {
			log.error("Error while reading xml file:"+path.toAbsolutePath());
			log.error(e);
			return Optional.empty();
		}
	}

}
