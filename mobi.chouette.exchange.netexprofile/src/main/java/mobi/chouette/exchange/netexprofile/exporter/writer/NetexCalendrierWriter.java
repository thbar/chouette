package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import org.rutebanken.netex.model.OperatingPeriod_VersionStructure;
import org.rutebanken.netex.model.ValidBetween;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.LocalDateTime;

import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.GENERAL_FRAME;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.MEMBERS;

public class NetexCalendrierWriter extends AbstractNetexWriter {

    public static void writer(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller, String timestamp, String typeNetex) throws XMLStreamException, JAXBException {
        writer.writeAttribute(ID,  NetexProducerUtils.createUniqueGeneralFrameId(context, GENERAL_FRAME, typeNetex, timestamp));

        ValidBetween validBetween = new ValidBetween();
        LocalDateTime fromDate = exportableNetexData.getSharedOperatingPeriods().values()
                .stream()
                .map(OperatingPeriod_VersionStructure::getFromDate)
                .min(LocalDateTime::compareTo)
                .get();

        LocalDateTime toDate = exportableNetexData.getSharedOperatingPeriods().values()
                .stream()
                .map(OperatingPeriod_VersionStructure::getToDate)
                .max(LocalDateTime::compareTo)
                .get();
        validBetween.setFromDate(fromDate);
        validBetween.setToDate(toDate);

        marshaller.marshal(validBetween, writer);

        TypeOfFrameWriter.typeOfFrameWriter(writer, marshaller, typeNetex);

        writer.writeStartElement(MEMBERS);

        ServiceCalendarFrameIDFMWriter.write(writer, context, exportableNetexData, marshaller);

        writer.writeEndElement();
    }
}
