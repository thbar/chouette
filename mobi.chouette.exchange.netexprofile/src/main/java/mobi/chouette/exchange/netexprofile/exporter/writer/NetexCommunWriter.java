package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import org.rutebanken.netex.model.Notice;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.util.Collection;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.exporter.writer.AbstractNetexWriter.ID;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.GENERAL_FRAME;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.MEMBERS;

public class NetexCommunWriter extends AbstractNetexWriter {

    public static void writer(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller, String timestamp, String typeNetex) throws XMLStreamException, JAXBException {
        writer.writeAttribute(ID, NetexProducerUtils.createUniqueGeneralFrameId(context, GENERAL_FRAME, typeNetex, timestamp));

        TypeOfFrameWriter.typeOfFrameWriter(writer, marshaller, typeNetex);

        if (!exportableNetexData.getSharedNotices().values().isEmpty()) {
            writer.writeStartElement(MEMBERS);

            writeNoticesElement(writer, exportableNetexData.getSharedNotices().values(), marshaller);

            writer.writeEndElement();
        }
    }

    private static void writeNoticesElement(XMLStreamWriter writer, Collection<Notice> notices, Marshaller marshaller) {
        try {
            if (!notices.isEmpty()) {
                for (Notice notice : notices) {
                    marshaller.marshal(netexFactory.createNotice(notice), writer);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
