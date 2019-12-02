package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.AbstractNorwayNetexProfileValidator;
import org.apache.commons.collections.MapUtils;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.RoutePoint;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.rutebanken.netex.model.ServiceLink;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Iterator;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.ADDITIONAL_NETWORKS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.DESTINATION_DISPLAYS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.DIRECTION;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.JOURNEY_PATTERNS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.LINES;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.LOC;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NOTICES;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NOTICE_ASSIGNMENTS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.OBJECT_ID_SPLIT_CHAR;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.ROUTE;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.ROUTE_POINTS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.SCHEDULED_STOP_POINTS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.SERVICE_LINKS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.STOP_ASSIGNMENTS;

public class AbstractNetexWriter {

    static final String NETEX_PROFILE_VERSION = AbstractNorwayNetexProfileValidator.EXPORT_PROFILE_ID;
    static final String DEFAULT_ZONE_ID = "Europe/Paris";
    static final String DEFAULT_LANGUAGE_CODE = "fr";
    static final String NSR_XMLNS = "MOSAIC";
	static final String PARTICIPANT_REF_CONTENT = "FR1";



    static final String VERSION = "version";
    static final String ID = "id";
    static final String CREATED = "created";
    static final String XMLNS = "Xmlns";
    static final String XMLNSURL = "XmlnsUrl";

    final static DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .optionalStart().appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true).optionalEnd()
            .optionalStart().appendPattern("XXXXX")
            .optionalEnd().toFormatter();

    static void writeElement(XMLStreamWriter writer, String element, String value) {
        try {
            writer.writeStartElement(element);
            writer.writeCharacters(value);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    static void writeNetworks(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) throws XMLStreamException {
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

    static void writeNoticesElement(XMLStreamWriter writer, Collection<Notice> notices, Marshaller marshaller) {
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

    static void writeNetworkElement(XMLStreamWriter writer, Network network, Marshaller marshaller) {
        try {
            marshaller.marshal(netexFactory.createNetwork(network), writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeDestinationDisplaysElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller, String codespace) {
        try {
            if (exportableData.getSharedDestinationDisplays().values().size() > 0) {
                for (DestinationDisplay destinationDisplay : exportableData.getSharedDestinationDisplays().values()) {
                    destinationDisplay.withId(codespace + OBJECT_ID_SPLIT_CHAR + DIRECTION + OBJECT_ID_SPLIT_CHAR + destinationDisplay.getId() + OBJECT_ID_SPLIT_CHAR + LOC);
                    destinationDisplay.withVersion(NETEX_DEFAULT_OBJECT_VERSION);
                    marshaller.marshal(netexFactory.createDestinationDisplay(destinationDisplay), writer);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeRoutePointsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
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

    static void writeRoutesElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller, String codespace) {
        try {
            for (org.rutebanken.netex.model.Route route : exportableData.getRoutes()) {
                route.withId(codespace + OBJECT_ID_SPLIT_CHAR + ROUTE + OBJECT_ID_SPLIT_CHAR + route.getId() + OBJECT_ID_SPLIT_CHAR + LOC);
                route.withVersion(NETEX_DEFAULT_OBJECT_VERSION);
                marshaller.marshal(netexFactory.createRoute(route), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeLinesElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
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

    static void writeScheduledStopPointsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
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

    static void writeServiceLinkElements(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
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

    static void writeStopAssignmentsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
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

    static void writeJourneyPatternsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
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

    static void writeNoticeAssignmentsElement(XMLStreamWriter writer, Collection<NoticeAssignment> noticeAssignments, Marshaller marshaller) {
        try {
            if (!noticeAssignments.isEmpty()) {
                writer.writeStartElement(NOTICE_ASSIGNMENTS);
                for (NoticeAssignment noticeAssignment : noticeAssignments) {
                    marshaller.marshal(netexFactory.createNoticeAssignment(noticeAssignment), writer);
                }
                writer.writeEndElement();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
