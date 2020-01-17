package mobi.chouette.exchange.netexprofile.exporter.writer;

import com.sun.org.apache.bcel.internal.classfile.LineNumber;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.NetexFragmentMode;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExportParameters;
import mobi.chouette.exchange.netexprofile.exporter.producer.LineProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import mobi.chouette.exchange.netexprofile.exporter.producer.RouteProducer;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.model.StopPoint;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.DestinationDisplayRefStructure;
import org.rutebanken.netex.model.Direction;
import org.rutebanken.netex.model.DirectionRefStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.RouteRefStructure;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static mobi.chouette.common.Constant.CONFIGURATION;
import static mobi.chouette.exchange.netexprofile.Constant.MARSHALLER;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;
import static org.jboss.util.property.jmx.SystemPropertyClassValue.log;
import static org.jboss.util.property.jmx.SystemPropertyClassValue.setSystemPropertyClassValue;


public class PublicationDeliveryIDFMWriterTest {


    private static LineProducer lineProducer = new LineProducer();
    private static RouteProducer routeProducer = new RouteProducer();

    @Test
    public void exportNetexOffreLigne() throws XMLStreamException, JAXBException {
        Context context = new Context();
        ExportableData exportableData = new ExportableData();
        ExportableNetexData exportableNetexData = new ExportableNetexData();

        mobi.chouette.model.Line line = new mobi.chouette.model.Line();
        line.setObjectId("TEST:Line:1");

        exportableData.setLine(line);

        Line_VersionStructure netexLine = new Line_VersionStructure();
        netexLine.setId("TEST:Line:1");
        exportableNetexData.setLine(netexLine);

        mobi.chouette.model.Route route = new mobi.chouette.model.Route();
        route.setObjectId("TEST:Route:1");
        route.setLine(line);
        route.setName("Route 1");

        org.rutebanken.netex.model.Route netexRoute = routeProducer.produce(context, route);

        DirectionRefStructure directionRefStructure = new DirectionRefStructure();
        directionRefStructure.setRef("TEST:Direction:1");
        directionRefStructure.setVersion("any");
        netexRoute.setDirectionRef(directionRefStructure);

        Direction direction = new Direction();
        direction.setId("TEST:Direction:1:LOC");
        direction.setVersion("any");
        MultilingualString testDirection = new MultilingualString();
        testDirection.setValue("TestDirection");
        direction.setName(testDirection);

        ServiceJourneyPattern serviceJourneyPattern = new ServiceJourneyPattern();
        MultilingualString multilingualStringJourneyPatternName = new MultilingualString();
        multilingualStringJourneyPatternName.setValue("Journey pattern 1");
        serviceJourneyPattern.setName(multilingualStringJourneyPatternName);

        RouteRefStructure routeRefStructure = new RouteRefStructure();
        routeRefStructure.setValue("TEST:Route:1");
        serviceJourneyPattern.setRouteRef(routeRefStructure);

        DestinationDisplayRefStructure destinationDisplayRefStructure = new DestinationDisplayRefStructure();
        destinationDisplayRefStructure.setRef("TEST:DestinationDisplay:1");
        destinationDisplayRefStructure.setVersion("any");
        serviceJourneyPattern.setDestinationDisplayRef(destinationDisplayRefStructure);

        PointsInJourneyPattern_RelStructure pointsInJourneyPattern_relStructure = new PointsInJourneyPattern_RelStructure();
        Collection<PointInLinkSequence_VersionedChildStructure> pointInLinkSequence_versionedChildStructures = new ArrayList<>();

        StopPointInJourneyPattern stopPointInJourneyPattern = new StopPointInJourneyPattern();
        stopPointInJourneyPattern.setId("TEST:StopPointInJourneyPattern:1:LOC");
        stopPointInJourneyPattern.setVersion("any");
        stopPointInJourneyPattern.setOrder(BigInteger.valueOf(1));

        ScheduledStopPointRefStructure scheduledStopPointRefStructure = netexFactory.createScheduledStopPointRefStructure();
        scheduledStopPointRefStructure.setRef("TEST:ScheduledStopPoint:1:LOC");
        scheduledStopPointRefStructure.setVersion("any");
        stopPointInJourneyPattern.setScheduledStopPointRef(netexFactory.createScheduledStopPointRef(scheduledStopPointRefStructure));

        pointInLinkSequence_versionedChildStructures.add(stopPointInJourneyPattern);


        pointsInJourneyPattern_relStructure.withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(pointInLinkSequence_versionedChildStructures);
        serviceJourneyPattern.setPointsInSequence(pointsInJourneyPattern_relStructure);

        DestinationDisplay destinationDisplay = new DestinationDisplay();
        destinationDisplay.setId("TEST:DestinationDisplay:1:LOC");
        destinationDisplay.setVersion("any");

        ScheduledStopPoint scheduledStopPoint = new ScheduledStopPoint();
        scheduledStopPoint.setId("TEST:ScheduledStopPoint:1:LOC");
        scheduledStopPoint.setVersion("any");

        PassengerStopAssignment passengerStopAssignment = new PassengerStopAssignment();
        passengerStopAssignment.setId("TEST:PassengerStopAssignment:1:LOC");
        passengerStopAssignment.setVersion("any");
        passengerStopAssignment.setOrder(BigInteger.valueOf(0));

        ScheduledStopPointRefStructure scheduledStopPointRef = netexFactory.createScheduledStopPointRefStructure();
        scheduledStopPointRef.setRef("TEST:ScheduledStopPoint:1:LOC");
        scheduledStopPointRef.setVersion("any");
        passengerStopAssignment.setScheduledStopPointRef(netexFactory.createScheduledStopPointRef(scheduledStopPointRef));

        QuayRefStructure quayRefStruct = netexFactory.createQuayRefStructure();

        passengerStopAssignment.setQuayRef(quayRefStruct);


        exportableNetexData.setLine(netexLine);
        exportableNetexData.getRoutes().add(netexRoute);
        exportableNetexData.getDirections().add(direction);
        exportableNetexData.getServiceJourneyPatterns().add(serviceJourneyPattern);
        exportableNetexData.getSharedDestinationDisplays().put(destinationDisplay.getId(), destinationDisplay);
        exportableNetexData.getSharedScheduledStopPoints().put(scheduledStopPoint.getId(), scheduledStopPoint);
        exportableNetexData.getSharedStopAssignments().put(passengerStopAssignment.getId(), passengerStopAssignment);


        NetexprofileExportParameters configuration = new NetexprofileExportParameters();
        configuration.setDefaultCodespacePrefix("TEST");
        context.put(CONFIGURATION, configuration);

        NetexXMLProcessingHelperFactory netexXMLFactory = new NetexXMLProcessingHelperFactory();
        context.put(MARSHALLER, netexXMLFactory.createFragmentMarshaller());

        Marshaller marshaller = (Marshaller) context.get(MARSHALLER);

        Path filePath = new File("Test-offre-ligne.xml").toPath();

        IndentingXMLStreamWriter writer = null;

        try {
            writer = NetexXMLProcessingHelperFactory.createXMLWriter(filePath);

            writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            PublicationDeliveryIDFMWriter.write(context, writer, exportableData, exportableNetexData, NetexFragmentMode.LINE, marshaller);

        } catch (XMLStreamException | IOException e) {
            log.error("Could not produce XML file", e);
            throw new RuntimeException(e);

        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (XMLStreamException e) {
                    log.error("Error flushing and closing Netex Export XML file " + filePath.toString(), e);
                    throw e;
                }
            }

        }

        List<String> content = null;
        try {
            content = Files.readAllLines(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < content.size(); i++) {
            System.out.println(content.get(i));
        }

        try {
            Files.delete(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void exportNetexCommun() {

    }

    @Test
    public void exportNetexHoraire() {

    }

}