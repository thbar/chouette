package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.NetexFragmentMode;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExportParameters;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.ScheduledStopPoint;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.GENERAL_FRAME;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.MEMBERS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_STRUCTURE;

public class NetexStructureWriter extends AbstractNetexWriter {

    public static void writer(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller, String timestamp, String typeNetex) throws XMLStreamException, JAXBException {
        writer.writeAttribute(ID, NetexProducerUtils.createUniqueGeneralFrameInLineId(context, typeNetex, timestamp));

        TypeOfFrameWriter.typeOfFrameWriter(writer, marshaller, typeNetex);

        writer.writeStartElement(MEMBERS);

        writeRoutesElement(writer, exportableNetexData, marshaller);
        writeDirectionsElement(writer, exportableNetexData, marshaller);
        writeServiceJourneyPatternsElement(writer, exportableNetexData, marshaller);
        writeScheduledStopPointsElement(writer, exportableNetexData, marshaller);
        writePassengerStopAssignmentsElement(writer, exportableNetexData, marshaller);
        writeDestinationDisplaysElement(writer, exportableNetexData, marshaller);

        writer.writeEndElement();
    }

    static void writeRoutesElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (org.rutebanken.netex.model.Route route : exportableNetexData.getRoutes()) {
                marshaller.marshal(netexFactory.createRoute(route), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeDirectionsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller){
        try {
            for (org.rutebanken.netex.model.Direction direction : exportableNetexData.getDirections()) {
                marshaller.marshal(netexFactory.createDirection(direction), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeServiceJourneyPatternsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller){
        try {
            for (org.rutebanken.netex.model.ServiceJourneyPattern serviceJourneyPattern : exportableNetexData.getServiceJourneyPattern()) {
                marshaller.marshal(netexFactory.createServiceJourneyPattern(serviceJourneyPattern), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeScheduledStopPointsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (ScheduledStopPoint scheduledStopPoint : exportableNetexData.getSharedScheduledStopPoints().values()) {
                marshaller.marshal(netexFactory.createScheduledStopPoint(scheduledStopPoint), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writePassengerStopAssignmentsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (PassengerStopAssignment stopAssignment : exportableNetexData.getSharedStopAssignments().values()) {
                marshaller.marshal(netexFactory.createPassengerStopAssignment(stopAssignment), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeDestinationDisplaysElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            if (exportableNetexData.getSharedDestinationDisplays().values().size() > 0) {
                for (DestinationDisplay destinationDisplay : exportableNetexData.getSharedDestinationDisplays().values()) {
                    marshaller.marshal(netexFactory.createDestinationDisplay(destinationDisplay), writer);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
