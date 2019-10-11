package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.NetexFragmentMode;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;

import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.GENERAL_FRAME;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_HORAIRE;

public class GeneralFrameWriter extends AbstractNetexWriter {

    public static void write(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, NetexFragmentMode fragmentMode,
                             Marshaller marshaller, String timestamp) {

        try {
            if (fragmentMode.equals(NetexFragmentMode.LINE)) {

                ElementsWriter.writeNetworks(writer, exportableNetexData, marshaller);

                ElementsWriter.writeLinesElement(writer, exportableNetexData, marshaller);

                ElementsWriter.writeRoutesElement(writer, exportableNetexData, marshaller);
                ElementsWriter.writeRoutePointsElement(writer, exportableNetexData, marshaller);

                ElementsWriter.writeJourneyPatternsElement(writer, exportableNetexData, marshaller);
                ElementsWriter.writeScheduledStopPointsElement(writer, exportableNetexData, marshaller);

                ElementsWriter.writeDestinationDisplaysElement(writer, exportableNetexData, marshaller);
                ElementsWriter.writeStopAssignmentsElement(writer, exportableNetexData, marshaller);
                ElementsWriter.writeServiceLinkElements(writer, exportableNetexData, marshaller);

                ReusedConstructsWriter.writeNoticeAssignmentsElement(writer, exportableNetexData.getNoticeAssignmentsServiceFrame(), marshaller);

                writer.writeEndElement();

                String generalFrameNetexHoraireId = NetexProducerUtils.createUniqueGeneralFrameInLineId(context, NETEX_HORAIRE, timestamp);
                writer.writeStartElement(GENERAL_FRAME);
                writer.writeAttribute(VERSION, NETEX_DEFAULT_OBJECT_VERSION);
                writer.writeAttribute(ID, generalFrameNetexHoraireId);

                TimetableFrameWriter.write(writer, context, exportableNetexData, marshaller);
            }

            if (fragmentMode.equals(NetexFragmentMode.COMMON)) {
               ElementsWriter.writeNoticesElement(writer, exportableNetexData.getSharedNotices().values(), marshaller);
            }

            if (fragmentMode.equals(NetexFragmentMode.CALENDAR)){
                ServiceCalendarFrameWriter.write(writer, context, exportableNetexData, marshaller);
            }


            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
