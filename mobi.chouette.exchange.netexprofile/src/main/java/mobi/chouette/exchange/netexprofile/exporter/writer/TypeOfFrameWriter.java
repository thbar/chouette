package mobi.chouette.exchange.netexprofile.exporter.writer;

import org.rutebanken.netex.model.TypeOfFrameRefStructure;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.exporter.writer.AbstractNetexWriter.PARTICIPANT_REF_CONTENT;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_STRUCTURE;

public class TypeOfFrameWriter {

    public static void typeOfFrameWriter(XMLStreamWriter writer, Marshaller marshaller, String typeOfFrame) throws JAXBException {
        TypeOfFrameRefStructure typeOfFrameRefStructure = new TypeOfFrameRefStructure();
        typeOfFrameRefStructure.withRef(PARTICIPANT_REF_CONTENT + ":TypeOfFrame:" + typeOfFrame + ":");
        typeOfFrameRefStructure.withValue("version=\"1.04:FR1-" + typeOfFrame + "-2.1\"");
        marshaller.marshal(netexFactory.createTypeOfFrameRef(typeOfFrameRefStructure), writer);
    }
}
