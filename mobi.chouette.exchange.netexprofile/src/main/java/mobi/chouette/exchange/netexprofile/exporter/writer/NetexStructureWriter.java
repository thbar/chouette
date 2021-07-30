package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.ScheduledStopPoint;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.MEMBERS;

public class NetexStructureWriter extends AbstractNetexWriter {

    public static void writer(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller, String timestamp, String typeNetex) throws XMLStreamException, JAXBException {
        writer.writeAttribute(ID, NetexProducerUtils.createUniqueGeneralFrameInLineId(context, typeNetex, timestamp));

        TypeOfFrameWriter.typeOfFrameWriter(writer, marshaller, typeNetex);

        writer.writeStartElement(MEMBERS);

        writeRouteLinksElement(writer, exportableNetexData, marshaller);
        writeRoutesElement(writer, exportableNetexData, marshaller);
        writeDirectionsElement(writer, exportableNetexData, marshaller);
        writeServiceJourneyPatternsElement(writer, exportableNetexData, marshaller);
        writeScheduledStopPointsElement(writer, exportableNetexData, marshaller);
        writePassengerStopAssignmentsElement(writer, exportableNetexData, marshaller);
        writeDestinationDisplaysElement(writer, exportableNetexData, marshaller);

        writer.writeEndElement();
    }

    static void writeRouteLinksElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (org.rutebanken.netex.model.RouteLink routeLink : exportableNetexData.getRouteLinks()) {
                marshaller.marshal(netexFactory.createRouteLink(routeLink), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            for (org.rutebanken.netex.model.ServiceJourneyPattern serviceJourneyPattern : exportableNetexData.getServiceJourneyPatterns()) {
                marshaller.marshal(netexFactory.createServiceJourneyPattern(serviceJourneyPattern), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeScheduledStopPointsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (ScheduledStopPoint scheduledStopPoint : exportableNetexData.getScheduledStopPoints().values()) {
                marshaller.marshal(netexFactory.createScheduledStopPoint(scheduledStopPoint), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writePassengerStopAssignmentsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (PassengerStopAssignment stopAssignment : exportableNetexData.getStopAssignments().values()) {
                marshaller.marshal(netexFactory.createPassengerStopAssignment(stopAssignment), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeDestinationDisplaysElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            if (exportableNetexData.getDestinationDisplays().values().size() > 0) {
                for (DestinationDisplay destinationDisplay : exportableNetexData.getDestinationDisplays().values()) {
                    marshaller.marshal(netexFactory.createDestinationDisplay(destinationDisplay), writer);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
