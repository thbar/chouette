package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.NetexFragmentMode;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;

import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.GENERAL_FRAME;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_CALENDAR;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_COMMUN;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_HORAIRE;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_STRUCTURE;

public class GeneralFrameWriter extends AbstractNetexWriter {

    public static void write(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, NetexFragmentMode fragmentMode,
                             Marshaller marshaller, String timestamp, String typeNetex) {

        String generalFrameId;

        try {

            writer.writeStartElement(GENERAL_FRAME);
            writer.writeAttribute(CREATED, timestamp);
            writer.writeAttribute(VERSION, NETEX_DEFAULT_OBJECT_VERSION);

            if(typeNetex.equals(NETEX_STRUCTURE)){
                generalFrameId = NetexProducerUtils.createUniqueGeneralFrameInLineId(context, NETEX_CALENDAR, timestamp);
                writer.writeAttribute(ID, generalFrameId);


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

            }

            if(typeNetex.equals(NETEX_HORAIRE)){
                generalFrameId = NetexProducerUtils.createUniqueGeneralFrameInLineId(context, NETEX_HORAIRE, timestamp);
                writer.writeAttribute(ID, generalFrameId);

                TimetableFrameWriter.write(writer, context, exportableNetexData, marshaller);

            }

            if(fragmentMode.equals(NetexFragmentMode.CALENDAR)){
                generalFrameId = NetexProducerUtils.createUniqueGeneralFrameId(context, GENERAL_FRAME, NETEX_CALENDAR, timestamp);
                writer.writeAttribute(ID, generalFrameId);

                ServiceCalendarFrameWriter.write(writer, context, exportableNetexData, marshaller);

            }

            if(fragmentMode.equals(NetexFragmentMode.COMMON)){
                generalFrameId = NetexProducerUtils.createUniqueGeneralFrameId(context, GENERAL_FRAME, NETEX_COMMUN, timestamp);
                writer.writeAttribute(ID, generalFrameId);

                ElementsWriter.writeNoticesElement(writer, exportableNetexData.getSharedNotices().values(), marshaller);

            }


            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

}
