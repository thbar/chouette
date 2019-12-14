package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import org.rutebanken.netex.model.ServiceJourney;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.GENERAL_FRAME;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.MEMBERS;

public class NetexHoraireWriter extends AbstractNetexWriter {

    public static void writer(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller, String timestamp, String typeNetex) throws XMLStreamException, JAXBException {
        writer.writeAttribute(ID, NetexProducerUtils.createUniqueGeneralFrameId(context, GENERAL_FRAME, typeNetex, timestamp));

        TypeOfFrameWriter.typeOfFrameWriter(writer, marshaller, typeNetex);

        writer.writeStartElement(MEMBERS);

        writeVehicleJourneysElement(writer, exportableNetexData, marshaller);

        writer.writeEndElement();
    }

    static void writeVehicleJourneysElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            if (exportableNetexData.getServiceJourneys() != null && exportableNetexData.getServiceJourneys().size() > 0){
                for (ServiceJourney serviceJourney : exportableNetexData.getServiceJourneys()) {
                    marshaller.marshal(netexFactory.createServiceJourney(serviceJourney), writer);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
