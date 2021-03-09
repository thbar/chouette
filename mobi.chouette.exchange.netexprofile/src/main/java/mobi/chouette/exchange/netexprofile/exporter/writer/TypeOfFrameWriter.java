package mobi.chouette.exchange.netexprofile.exporter.writer;

import org.rutebanken.netex.model.TypeOfFrameRefStructure;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.exporter.writer.AbstractNetexWriter.PARTICIPANT_REF_CONTENT;

public class TypeOfFrameWriter {

    public static void typeOfFrameWriter(XMLStreamWriter writer, Marshaller marshaller, String typeOfFrame) throws JAXBException {
        TypeOfFrameRefStructure typeOfFrameRefStructure = new TypeOfFrameRefStructure();
        typeOfFrameRefStructure.withRef(PARTICIPANT_REF_CONTENT + ":TypeOfFrame:" + typeOfFrame + ":");
        typeOfFrameRefStructure.withValue("version=\"1.1:FR-" + typeOfFrame + "-2.2\"");
        marshaller.marshal(netexFactory.createTypeOfFrameRef(typeOfFrameRefStructure), writer);
    }
}
