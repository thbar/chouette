package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import org.apache.commons.collections.MapUtils;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.RoutePoint;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.rutebanken.netex.model.ServiceLink;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Collection;
import java.util.Iterator;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.ADDITIONAL_NETWORKS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.DESTINATION_DISPLAYS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.JOURNEY_PATTERNS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.LINES;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NOTICES;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.ROUTES;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.ROUTE_POINTS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.SCHEDULED_STOP_POINTS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.SERVICE_LINKS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.STOP_ASSIGNMENTS;

public class ElementsWriter {

    public static void writeNetworks(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) throws XMLStreamException {
        if (!exportableNetexData.getSharedNetworks().isEmpty()) {
            Iterator<Network> networkIterator=exportableNetexData.getSharedNetworks().values().iterator();
            writeNetworkElement(writer, networkIterator.next(), marshaller);

            if (networkIterator.hasNext()){
                writer.writeStartElement(ADDITIONAL_NETWORKS);
                while(networkIterator.hasNext()) {
                    writeNetworkElement(writer, networkIterator.next(), marshaller);
                }
                writer.writeEndElement();
            }
        }
    }

    public static void writeNoticesElement(XMLStreamWriter writer, Collection<Notice> notices, Marshaller marshaller) {
        try {
            if (!notices.isEmpty()) {
                writer.writeStartElement(NOTICES);
                for (Notice notice : notices) {
                    marshaller.marshal(netexFactory.createNotice(notice), writer);
                }
                writer.writeEndElement();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeNetworkElement(XMLStreamWriter writer, Network network, Marshaller marshaller) {
        try {
            marshaller.marshal(netexFactory.createNetwork(network), writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeDestinationDisplaysElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            if (exportableData.getSharedDestinationDisplays().values().size() > 0) {
                writer.writeStartElement(DESTINATION_DISPLAYS);
                for (DestinationDisplay destinationDisplay : exportableData.getSharedDestinationDisplays().values()) {
                    marshaller.marshal(netexFactory.createDestinationDisplay(destinationDisplay), writer);
                }
                writer.writeEndElement();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeRoutePointsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            if (!MapUtils.isEmpty(exportableData.getSharedRoutePoints())) {
                writer.writeStartElement(ROUTE_POINTS);
                for (RoutePoint routePoint : exportableData.getSharedRoutePoints().values()) {
                    marshaller.marshal(netexFactory.createRoutePoint(routePoint), writer);
                }
                writer.writeEndElement();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeRoutesElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            writer.writeStartElement(ROUTES);
            for (org.rutebanken.netex.model.Route route : exportableData.getRoutes()) {
                marshaller.marshal(netexFactory.createRoute(route), writer);
            }
            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeLinesElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            writer.writeStartElement(LINES);
            Line_VersionStructure line = exportableData.getLine();
            JAXBElement<? extends Line_VersionStructure> jaxbElement = null;
            if (line instanceof Line) {
                jaxbElement = netexFactory.createLine((Line) exportableData.getLine());
            } else if (line instanceof FlexibleLine) {
                jaxbElement = netexFactory.createFlexibleLine((FlexibleLine) exportableData.getLine());
            }
            marshaller.marshal(jaxbElement, writer);
            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeScheduledStopPointsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            writer.writeStartElement(SCHEDULED_STOP_POINTS);
            for (ScheduledStopPoint scheduledStopPoint : exportableData.getSharedScheduledStopPoints().values()) {
                marshaller.marshal(netexFactory.createScheduledStopPoint(scheduledStopPoint), writer);
            }
            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeServiceLinkElements(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            if (!MapUtils.isEmpty(exportableData.getSharedServiceLinks())) {
                writer.writeStartElement(SERVICE_LINKS);
                for (ServiceLink serviceLink : exportableData.getSharedServiceLinks().values()) {
                    marshaller.marshal(netexFactory.createServiceLink(serviceLink), writer);
                }
                writer.writeEndElement();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeStopAssignmentsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            writer.writeStartElement(STOP_ASSIGNMENTS);
            for (PassengerStopAssignment stopAssignment : exportableData.getSharedStopAssignments().values()) {
                marshaller.marshal(netexFactory.createPassengerStopAssignment(stopAssignment), writer);
            }
            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeJourneyPatternsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            writer.writeStartElement(JOURNEY_PATTERNS);
            for (JourneyPattern journeyPattern : exportableData.getJourneyPatterns()) {
                marshaller.marshal(netexFactory.createJourneyPattern(journeyPattern), writer);
            }
            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
