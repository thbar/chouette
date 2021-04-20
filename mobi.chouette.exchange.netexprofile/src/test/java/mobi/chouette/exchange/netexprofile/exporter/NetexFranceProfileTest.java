package mobi.chouette.exchange.netexprofile.exporter;


import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.JobDataTest;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.model.Company;
import mobi.chouette.model.DestinationDisplay;
import mobi.chouette.model.Footnote;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.Period;
import mobi.chouette.model.Route;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.SimpleObjectReference;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.PTDirectionEnum;
import org.joda.time.LocalDate;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.CompositeFrame;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.Direction;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.General_VersionFrameStructure;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.NoticeRefStructure;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TypeOfFrameRefStructure;
import org.rutebanken.netex.validation.NeTExValidator;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static mobi.chouette.common.Constant.*;
import static mobi.chouette.exchange.netexprofile.Constant.MARSHALLER;

public class NetexFranceProfileTest {

    private static String codifLigne = "TestNetexFranceProfile";
    private static String testPath = "src/test/data/netexFranceProfile";
    private static String generatedFilePath = testPath+"/output/offre_"+codifLigne+"_.xml";
    private File generatedFile = new File(generatedFilePath);

    private NetexXMLProcessingHelperFactory importer = new NetexXMLProcessingHelperFactory();




    @Test
    public void exportOffreIDFM() throws Exception {
        deleteFileCreated();

        Context context = createContext();

        context.put(CREATION_DATE,LocalDateTime.now());


        NetexLineProducerCommand lineProducer = new NetexLineProducerCommand();
        lineProducer.execute(context);

        checkGeneratedFile();



      //  deleteFileCreated();

    }

    private void checkGeneratedFile(){

        PublicationDeliveryStructure lineDeliveryStructure;
        try {
            lineDeliveryStructure = importer.unmarshal(generatedFile,new HashSet<>());
        } catch (JAXBException|XMLStreamException|IOException|SAXException e) {
            Assert.fail("Unable to unmarshal generated file");
            System.out.println(e);
            return;
        }
        lineDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame().get(0).getValue();

        Assert.assertEquals(lineDeliveryStructure.getVersion(), "1.1:FR-NETEX-2.2-z", "wrong version");

        Assert.assertEquals(lineDeliveryStructure.getParticipantRef(), "TEST", "wrong participant REF");


        List<CompositeFrame> compositeFrames = getCompositeFrames(lineDeliveryStructure);


        for (CompositeFrame compositeFrame : compositeFrames) {


            Assert.assertEquals(compositeFrame.getVersion(), "any", "wrong version");
            Assert.assertEquals(compositeFrame.getId(), "TEST:CompositeFrame:NETEX_RESEAUX-LOC", "wrong ID");
            Assert.assertEquals(compositeFrame.getName().getValue(), "TestLineName", "wrong line name");

            TypeOfFrameRefStructure compositeTypeOfFrame = compositeFrame.getTypeOfFrameRef();
            Assert.assertEquals(compositeTypeOfFrame.getRef(), "FR:TypeOfFrame:NETEX_RESEAUX:", "wrong ref");
            Assert.assertEquals(compositeTypeOfFrame.getValue(), "version=\"1.1:FR-NETEX_RESEAUX-2.2\"", "wrong value");




            List<GeneralFrame> generalFrames = getGeneralFrames(compositeFrame);

            /**
             * FIRST GENERAL FRAME
             */
            GeneralFrame firstFrame = generalFrames.get(0);
            Assert.assertEquals(firstFrame.getVersion(), "any", "wrong version");
            Assert.assertTrue(firstFrame.getId().startsWith("TEST:NETEX_LIGNE-"), "wrong id");
            Assert.assertTrue(firstFrame.getId().endsWith(":LOC"), "wrong id");
            TypeOfFrameRefStructure firstTypeOfFrameRef = firstFrame.getTypeOfFrameRef();
            Assert.assertEquals(firstTypeOfFrameRef.getRef(), "FR1:TypeOfFrame:NETEX_LIGNE:", "wrong ref");
            Assert.assertEquals(firstTypeOfFrameRef.getValue(), "version=\"1.1:FR-NETEX_LIGNE-2.2\"", "wrong value");

            General_VersionFrameStructure.Members members = firstFrame.getMembers();

            List<org.rutebanken.netex.model.Route> routes = getRoutes(members);
            if (routes.size() > 0){
                checkRoutes(routes);
            }

            List<org.rutebanken.netex.model.Direction> directionList = getDirection(members);
            if (directionList.size() > 0){
                checkDirection(directionList);
            }

            List<ServiceJourneyPattern> serviceJourneyPatterns = getServiceJourneyPatterns(members);
            if (serviceJourneyPatterns.size() > 0){
                checkServiceJourneyPatterns(serviceJourneyPatterns);
            }

            List<org.rutebanken.netex.model.ScheduledStopPoint> scheduledStopPoints = getScheduledStopPointPatterns(members);
            if (scheduledStopPoints.size() > 0){
                checkScheduledStopPoints(scheduledStopPoints);
            }

            List<org.rutebanken.netex.model.PassengerStopAssignment> passengerStopAssignmentList = getPassengerStopAssignment(members);
            if (passengerStopAssignmentList.size() > 0){
                checkPassengerStopAssignment(passengerStopAssignmentList);
            }

            List<org.rutebanken.netex.model.DestinationDisplay> destinationDisplayList = getDestinationDisplay(members);
            if (destinationDisplayList.size() > 0){
                checkDestinationDisplay(destinationDisplayList);
            }



            /**
             * SECOND GENERAL FRAME
             */
            GeneralFrame secondFrame = generalFrames.get(1);
            Assert.assertEquals(secondFrame.getVersion(), "any", "wrong version");
            Assert.assertTrue(secondFrame.getId().startsWith("TEST:NETEX_HORAIRE-"), "wrong id");
            Assert.assertTrue(secondFrame.getId().endsWith(":LOC"), "wrong id");

            TypeOfFrameRefStructure secondTypeOfFrameRef = secondFrame.getTypeOfFrameRef();
            Assert.assertEquals(secondTypeOfFrameRef.getRef(), "FR1:TypeOfFrame:NETEX_HORAIRE:", "wrong ref");
            Assert.assertEquals(secondTypeOfFrameRef.getValue(), "version=\"1.1:FR-NETEX_HORAIRE-2.2\"", "wrong value");

            ServiceJourney serviceJourney = (ServiceJourney) secondFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity().get(0).getValue();
            Assert.assertEquals(serviceJourney.getVersion(), "any", "wrong version");
            Assert.assertEquals(serviceJourney.getId(), "TEST:ServiceJourney:vj1:LOC", "wrong id");
            Assert.assertEquals(serviceJourney.getName().getValue(), "Test vehicle journey name", "wrong name");

            NoticeAssignment noticeAssignment = (NoticeAssignment) serviceJourney.getNoticeAssignments().getNoticeAssignment_OrNoticeAssignmentView().get(0).getValue();
            Assert.assertEquals(noticeAssignment.getVersion(), "any", "wrong version");
            Assert.assertEquals(noticeAssignment.getOrder(), new BigInteger("0"), "wrong order");
            Assert.assertEquals(noticeAssignment.getId(), "TEST:NoticeAssignment:1:LOC", "wrong id");

            NoticeRefStructure noticeRef = noticeAssignment.getNoticeRef();
            Assert.assertEquals(noticeRef.getRef(), "TEST:Notice:f1:LOC", "wrong ref");
            Assert.assertEquals(noticeRef.getValue(), "version=\"any\"", "wrong value");


            DayTypeRefStructure firstDayType = serviceJourney.getDayTypes().getDayTypeRef().get(0).getValue();

            Assert.assertEquals(firstDayType.getRef(), "TEST:DayType:t1:LOC", "wrong day type ref");
            Assert.assertEquals(firstDayType.getValue(), "version=\"any\"", "wrong value ");
            
            
            
            JourneyPatternRefStructure journeyPatternRef = serviceJourney.getJourneyPatternRef().getValue();
            Assert.assertEquals(journeyPatternRef.getRef(), "TEST:ServiceJourneyPattern:jp1:LOC", "wrong ref");
            Assert.assertEquals(journeyPatternRef.getVersion(), "any", "wrong version");

            List<TimetabledPassingTime> passingTimes = serviceJourney.getPassingTimes().getTimetabledPassingTime();

            TimetabledPassingTime firstPassingTime = passingTimes.get(0);
            Assert.assertEquals(firstPassingTime.getVersion(), "any", "wrong version");



            Assert.assertEquals(firstPassingTime.getArrivalTime(), LocalTime.of(7,0,0), "wrong arrivalTime");
            Assert.assertEquals(firstPassingTime.getDepartureTime(),LocalTime.of(7,0,0), "wrong departureTime");

            TimetabledPassingTime secondPassingTime = passingTimes.get(1);
            Assert.assertEquals(secondPassingTime.getVersion(), "any", "wrong version");
            Assert.assertEquals(secondPassingTime.getArrivalTime(), LocalTime.of(7,15,0), "wrong arrivalTime");
            Assert.assertEquals(secondPassingTime.getDepartureTime(), LocalTime.of(7,15,0), "wrong departureTime");

            TimetabledPassingTime thirdPassingTime = passingTimes.get(2);
            Assert.assertEquals(thirdPassingTime.getVersion(), "any", "wrong version");
            Assert.assertEquals(thirdPassingTime.getArrivalTime(), LocalTime.of(7,30,0), "wrong arrivalTime");
            Assert.assertEquals(thirdPassingTime.getDepartureTime(), LocalTime.of(7,30,0), "wrong departureTime");

        }
    }

    private void checkDirection(List<org.rutebanken.netex.model.Direction> directionList){
        Direction firstDirection = directionList.get(0);
        Assert.assertEquals(firstDirection.getVersion(), "any", "wrong version");
        Assert.assertEquals(firstDirection.getId(), "TEST:Direction:r1:LOC", "wrong id");
        Assert.assertEquals(firstDirection.getName().getValue(), "quay3", "wrong directionName");

    }


    private List<org.rutebanken.netex.model.Direction> getDirection(General_VersionFrameStructure.Members members){
        return members.getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity().stream()
                .map(JAXBElement::getValue)
                .filter(member -> member instanceof org.rutebanken.netex.model.Direction)
                .map(member -> (org.rutebanken.netex.model.Direction)member )
                .collect(Collectors.toList());

    }


    private void checkDestinationDisplay(List<org.rutebanken.netex.model.DestinationDisplay> destinationDisplayList){

        org.rutebanken.netex.model.DestinationDisplay firstDestinationDisplay = destinationDisplayList.get(0);
        Assert.assertEquals(firstDestinationDisplay.getVersion(), "any", "wrong version");
        Assert.assertEquals(firstDestinationDisplay.getId(), "TEST:DestinationDisplay:dd1:LOC", "wrong Id");
        Assert.assertEquals(firstDestinationDisplay.getFrontText().getValue(), "Test Destination Display", "wrong frontText");

    }


    private List<org.rutebanken.netex.model.DestinationDisplay> getDestinationDisplay(General_VersionFrameStructure.Members members){
        return members.getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity().stream()
                .map(JAXBElement::getValue)
                .filter(member -> member instanceof org.rutebanken.netex.model.DestinationDisplay)
                .map(member -> (org.rutebanken.netex.model.DestinationDisplay)member )
                .collect(Collectors.toList());

    }

    private void checkPassengerStopAssignment(List<org.rutebanken.netex.model.PassengerStopAssignment> passengerStopAssignmentList){

        PassengerStopAssignment firstAssignment = passengerStopAssignmentList.get(0);
        Assert.assertEquals(firstAssignment.getOrder(), new BigInteger("0"), "wrong order");
        Assert.assertEquals(firstAssignment.getVersion(), "any", "wrong version");
        Assert.assertEquals(firstAssignment.getId(), "TEST:PassengerStopAssignment:ssp3:LOC", "wrong id");
        ScheduledStopPointRefStructure firstScheduledPointRef = firstAssignment.getScheduledStopPointRef().getValue();
        Assert.assertEquals(firstScheduledPointRef.getRef(), "TEST:ScheduledStopPoint:ssp3:LOC", "wrong ref");
        Assert.assertEquals(firstScheduledPointRef.getVersion(), "any", "wrong version");
        QuayRefStructure quayRef = firstAssignment.getQuayRef();
        Assert.assertEquals(quayRef.getRef(), "TEST:Quay:quay3", "wrong ref");
        Assert.assertEquals(quayRef.getValue(), "version=\"any\"", "wrong value");

        PassengerStopAssignment secondAssignment = passengerStopAssignmentList.get(1);
        Assert.assertEquals(secondAssignment.getOrder(), new BigInteger("0"), "wrong order");
        Assert.assertEquals(secondAssignment.getVersion(), "any", "wrong version");
        Assert.assertEquals(secondAssignment.getId(), "TEST:PassengerStopAssignment:ssp2:LOC", "wrong id");
        ScheduledStopPointRefStructure secondScheduledPointRef = secondAssignment.getScheduledStopPointRef().getValue();
        Assert.assertEquals(secondScheduledPointRef.getRef(), "TEST:ScheduledStopPoint:ssp2:LOC", "wrong ref");
        Assert.assertEquals(secondScheduledPointRef.getVersion(), "any", "wrong version");
        QuayRefStructure secondQuayRef = secondAssignment.getQuayRef();
        Assert.assertEquals(secondQuayRef.getRef(), "TEST:Quay:quay2", "wrong ref");
        Assert.assertEquals(secondQuayRef.getValue(), "version=\"any\"", "wrong value");

        PassengerStopAssignment thirdAssignment = passengerStopAssignmentList.get(2);
        Assert.assertEquals(thirdAssignment.getOrder(), new BigInteger("0"), "wrong order");
        Assert.assertEquals(thirdAssignment.getVersion(), "any", "wrong version");
        Assert.assertEquals(thirdAssignment.getId(), "TEST:PassengerStopAssignment:ssp1:LOC", "wrong id");
        ScheduledStopPointRefStructure thirdScheduledPointRef = thirdAssignment.getScheduledStopPointRef().getValue();
        Assert.assertEquals(thirdScheduledPointRef.getRef(), "TEST:ScheduledStopPoint:ssp1:LOC", "wrong ref");
        Assert.assertEquals(thirdScheduledPointRef.getVersion(), "any", "wrong version");
        QuayRefStructure thirdQuayRef = thirdAssignment.getQuayRef();
        Assert.assertEquals(thirdQuayRef.getRef(), "TEST:Quay:quay1", "wrong ref");
        Assert.assertEquals(thirdQuayRef.getValue(), "version=\"any\"", "wrong value");

    }

    private List<org.rutebanken.netex.model.PassengerStopAssignment> getPassengerStopAssignment(General_VersionFrameStructure.Members members){
        return members.getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity().stream()
                .map(JAXBElement::getValue)
                .filter(member -> member instanceof org.rutebanken.netex.model.PassengerStopAssignment)
                .map(member -> (org.rutebanken.netex.model.PassengerStopAssignment)member )
                .collect(Collectors.toList());

    }


    private void checkScheduledStopPoints(List<org.rutebanken.netex.model.ScheduledStopPoint> scheduledStopPointList){
        org.rutebanken.netex.model.ScheduledStopPoint firstPoint = scheduledStopPointList.get(0);
        Assert.assertEquals(firstPoint.getId(), "TEST:ScheduledStopPoint:ssp2:LOC", "wrong id in first point");
        Assert.assertEquals(firstPoint.getVersion(), "any", "wrong version in first point");
        LocationStructure firstLocation = firstPoint.getLocation();
        Assert.assertEquals(firstLocation.getLatitude(), new BigDecimal("1.1111"), "wrong lat in first point");
        Assert.assertEquals(firstLocation.getLongitude(), new BigDecimal("2.2222"), "wrong long in first point");

        org.rutebanken.netex.model.ScheduledStopPoint secondPoint = scheduledStopPointList.get(1);
        Assert.assertEquals(secondPoint.getId(), "TEST:ScheduledStopPoint:ssp1:LOC", "wrong id in second point");
        Assert.assertEquals(secondPoint.getVersion(), "any", "wrong version in second point");
        LocationStructure secondLocation = secondPoint.getLocation();
        Assert.assertEquals(secondLocation.getLatitude(), new BigDecimal("4.4444"), "wrong lat in second point");
        Assert.assertEquals(secondLocation.getLongitude(), new BigDecimal("5.5555"), "wrong long in second point");

        org.rutebanken.netex.model.ScheduledStopPoint thirdPoint = scheduledStopPointList.get(2);
        Assert.assertEquals(thirdPoint.getId(), "TEST:ScheduledStopPoint:ssp3:LOC", "wrong id in third point");
        Assert.assertEquals(thirdPoint.getVersion(), "any", "wrong version in third point");
        LocationStructure thirdLocation = thirdPoint.getLocation();
        Assert.assertEquals(thirdLocation.getLatitude(), new BigDecimal("1.12345"), "wrong lat in third point");
        Assert.assertEquals(thirdLocation.getLongitude(), new BigDecimal("2.12345"), "wrong long in third point");

    }

    private List<org.rutebanken.netex.model.ScheduledStopPoint> getScheduledStopPointPatterns(General_VersionFrameStructure.Members members){
        return members.getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity().stream()
                .map(JAXBElement::getValue)
                .filter(member -> member instanceof org.rutebanken.netex.model.ScheduledStopPoint)
                .map(member -> (org.rutebanken.netex.model.ScheduledStopPoint)member )
                .collect(Collectors.toList());

    }


    private void checkServiceJourneyPatterns(List<ServiceJourneyPattern> serviceJourneyPatterns){
        ServiceJourneyPattern firstServiceJourneyPattern = serviceJourneyPatterns.get(0);
        Assert.assertEquals(firstServiceJourneyPattern.getName().getValue(), "Test Journey Pattern", "wrong name in Service JourneyPattern");
        Assert.assertEquals(firstServiceJourneyPattern.getRouteRef().getRef(), "TEST:Route:r1:LOC", "wrong routeRef in ServiceJourneyPattern");
        Assert.assertEquals(firstServiceJourneyPattern.getRouteRef().getVersion(), "any", "wrong version in routeRef");
        Assert.assertEquals(firstServiceJourneyPattern.getDestinationDisplayRef().getRef(), "TEST:DestinationDisplay:dd1:LOC", "wrong destinationDisplayRef in ServiceJourneyPattern");
        Assert.assertEquals(firstServiceJourneyPattern.getDestinationDisplayRef().getVersion(), "any", "wrong version in destinationDisplayRef");
        Assert.assertEquals(firstServiceJourneyPattern.getServiceJourneyPatternType().value(), "passenger", "wrong pattern type in destinationDisplayRef");

        StopPointInJourneyPattern firstPoint = (StopPointInJourneyPattern) firstServiceJourneyPattern.getPointsInSequence()
                                                                                                     .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                                                                                                     .get(0);

        Assert.assertEquals(firstPoint.getOrder(), new BigInteger("1"), "wrong order in first point");
        Assert.assertEquals(firstPoint.getVersion(), "any", "wrong version in first point");
        Assert.assertEquals(firstPoint.getId(), "TEST:StopPointInJourneyPattern:sp1:LOC", "wrong id in first point");
        Assert.assertEquals(firstPoint.getScheduledStopPointRef().getValue().getRef(), "TEST:ScheduledStopPoint:ssp1:LOC", "wrong scheduled stop point Ref in first point");
        Assert.assertEquals(firstPoint.getScheduledStopPointRef().getValue().getVersion(), "any", "wrong version in scheduled stop point Ref");


        StopPointInJourneyPattern secondPoint = (StopPointInJourneyPattern) firstServiceJourneyPattern.getPointsInSequence()
                                                                                                    .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                                                                                                    .get(1);

        Assert.assertEquals(secondPoint.getOrder(), new BigInteger("2"), "wrong order in second point");
        Assert.assertEquals(secondPoint.getVersion(), "any", "wrong version in second point");
        Assert.assertEquals(secondPoint.getId(), "TEST:StopPointInJourneyPattern:sp2:LOC", "wrong id in second point");
        Assert.assertEquals(secondPoint.getScheduledStopPointRef().getValue().getRef(), "TEST:ScheduledStopPoint:ssp2:LOC", "wrong scheduled stop point Ref in second point");
        Assert.assertEquals(secondPoint.getScheduledStopPointRef().getValue().getVersion(), "any", "wrong version in scheduled stop point Ref");

        StopPointInJourneyPattern thirdPoint = (StopPointInJourneyPattern) firstServiceJourneyPattern.getPointsInSequence()
                                                                                                    .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                                                                                                    .get(2);

        Assert.assertEquals(thirdPoint.getOrder(), new BigInteger("3"), "wrong order in third point");
        Assert.assertEquals(thirdPoint.getVersion(), "any", "wrong version in third point");
        Assert.assertEquals(thirdPoint.getId(), "TEST:StopPointInJourneyPattern:sp3:LOC", "wrong id in third point");
        Assert.assertEquals(thirdPoint.getScheduledStopPointRef().getValue().getRef(), "TEST:ScheduledStopPoint:ssp3:LOC", "wrong scheduled stop point Ref in third point");
        Assert.assertEquals(thirdPoint.getScheduledStopPointRef().getValue().getVersion(), "any", "wrong version in scheduled stop point Ref");


        System.out.println("a");

    }

    private List<ServiceJourneyPattern> getServiceJourneyPatterns(General_VersionFrameStructure.Members members){
        return members.getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity().stream()
                .map(JAXBElement::getValue)
                .filter(member -> member instanceof org.rutebanken.netex.model.ServiceJourneyPattern)
                .map(member -> (org.rutebanken.netex.model.ServiceJourneyPattern)member )
                .collect(Collectors.toList());

    }

    private void checkRoutes(List<org.rutebanken.netex.model.Route> routes){
        org.rutebanken.netex.model.Route firstRoute = routes.get(0);
        Assert.assertEquals(firstRoute.getId(), "TEST:Route:r1:LOC", "wrong id in generated route");
        Assert.assertEquals(firstRoute.getVersion(), "any", "wrong version in generated route");
        Assert.assertEquals(firstRoute.getName().getValue(), "Test Route", "wrong name in generated route");

        LineRefStructure lineRef = firstRoute.getLineRef().getValue();
        Assert.assertEquals(lineRef.getRef(), "TEST:Line:l1", "wrong lineRef in generated route");

        Assert.assertEquals( firstRoute.getDirectionRef().getRef(),"TEST:Direction:r1:LOC", "wrong direction ref");
        Assert.assertEquals( firstRoute.getDirectionType().value(),"outbound", "wrong direction type");

    }

    private List<org.rutebanken.netex.model.Route> getRoutes(General_VersionFrameStructure.Members members){
        return members.getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity().stream()
                                        .map(JAXBElement::getValue)
                                        .filter(member -> member instanceof org.rutebanken.netex.model.Route)
                                        .map(member -> (org.rutebanken.netex.model.Route)member )
                                        .collect(Collectors.toList());

    }

    private List<GeneralFrame> getGeneralFrames(CompositeFrame compositeFrame){
        return compositeFrame.getFrames().getCommonFrame().stream()
                                            .map(frame ->{
                                                if (frame.getValue() instanceof GeneralFrame){
                                                    return (GeneralFrame)frame.getValue();
                                                }else{
                                                    return null;
                                                }
                                            })
                                        .filter(frame -> frame != null)
                                        .collect(Collectors.toList());
    }


    private List<CompositeFrame> getCompositeFrames(PublicationDeliveryStructure lineDeliveryStructure){
       return  lineDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame()
                                                .stream()
                                                .map(frame -> {
                                                        if (frame.getValue() instanceof CompositeFrame){
                                                            return (CompositeFrame)frame.getValue();
                                                        }else{
                                                            return null;
                                                        }
                                                })
                                            .filter(frame -> frame != null)
                                            .collect(Collectors.toList());

    }



    private void deleteFileCreated() {
        File file = new File(generatedFilePath);
        if(file.delete()){
            System.out.println("Fichier de test supprimé");
        }else{
            System.out.println("ERREUR fichier de test non supprimé");
        }
    }

    private Context createContext() throws JAXBException {

        File file = new File(generatedFilePath);
        file.getParentFile().mkdirs();


        Line line = new Line();
        line.setObjectId("TEST:Line:l1");
        line.setRegistrationNumber("l1");
        line.setCodifligne(codifLigne);
        line.setName("TestLineName");
        line.setPublishedName("testPublishedName");

        Network network = new Network();
        Company company = new Company();
        company.setCode("myComp");
        company.setEmail("email@okinatest.com");
        company.setFareUrl("www.okinatest.com");
        company.setName("Okina test");

        network.setCompany(company);
        line.setNetwork(network);

        Route route  = new Route();
        route.setObjectId("TEST:Route:r1");
        route.setName("Test Route");
        route.setLine(line);
        route.setDirection(PTDirectionEnum.A);
        route.setPublishedName("publishedNameRoute");


        DestinationDisplay destinationDisplay = new DestinationDisplay();
        destinationDisplay.setObjectId("TEST:DestinationDisplay:dd1");
        destinationDisplay.setFrontText("Test Destination Display");

        StopArea stopArea1 = new StopArea();
        stopArea1.setObjectId("TEST:Quay:quay1");
        stopArea1.setAreaType(ChouetteAreaEnum.Quay);
        stopArea1.setLatitude(new BigDecimal("4.4444"));
        stopArea1.setLongitude(new BigDecimal("5.5555"));
        stopArea1.setName("quay1");


        StopArea stopArea2 = new StopArea();
        stopArea2.setObjectId("TEST:Quay:quay2");
        stopArea2.setAreaType(ChouetteAreaEnum.Quay);
        stopArea2.setLatitude(new BigDecimal("1.1111"));
        stopArea2.setLongitude(new BigDecimal("2.2222"));
        stopArea2.setName("quay2");


        StopArea stopArea3 = new StopArea();
        stopArea3.setObjectId("TEST:Quay:quay3");
        stopArea3.setAreaType(ChouetteAreaEnum.Quay);
        stopArea3.setLatitude(new BigDecimal("1.12345"));
        stopArea3.setLongitude(new BigDecimal("2.12345"));
        stopArea3.setName("quay3");


        ScheduledStopPoint scheduledStopPoint1 = new ScheduledStopPoint();
        scheduledStopPoint1.setObjectId("TEST:ScheduledStopPoint:ssp1");
        scheduledStopPoint1.setContainedInStopAreaRef(new SimpleObjectReference(stopArea1));

        ScheduledStopPoint scheduledStopPoint2 = new ScheduledStopPoint();
        scheduledStopPoint2.setObjectId("TEST:ScheduledStopPoint:ssp2");
        scheduledStopPoint2.setContainedInStopAreaRef(new SimpleObjectReference(stopArea2));

        ScheduledStopPoint scheduledStopPoint3 = new ScheduledStopPoint();
        scheduledStopPoint3.setObjectId("TEST:ScheduledStopPoint:ssp3");
        scheduledStopPoint3.setContainedInStopAreaRef(new SimpleObjectReference(stopArea3));


        StopPoint stopPoint1 = new StopPoint();
        stopPoint1.setObjectId("TEST:StopPoint:sp1");
        stopPoint1.setPosition(0);
        stopPoint1.setScheduledStopPoint(scheduledStopPoint1);
        stopPoint1.setRoute(route);

        StopPoint stopPoint2 = new StopPoint();
        stopPoint2.setObjectId("TEST:StopPoint:sp2");
        stopPoint2.setPosition(1);
        stopPoint2.setScheduledStopPoint(scheduledStopPoint2);
        stopPoint2.setRoute(route);

        StopPoint stopPoint3 = new StopPoint();
        stopPoint3.setObjectId("TEST:StopPoint:sp3");
        stopPoint3.setPosition(2);
        stopPoint3.setScheduledStopPoint(scheduledStopPoint3);
        stopPoint3.setDestinationDisplay(destinationDisplay);
        stopPoint3.setRoute(route);

        JourneyPattern journeyPattern = new JourneyPattern();
        journeyPattern.setObjectId("TEST:JourneyPattern:jp1");
        journeyPattern.setName("Test Journey Pattern");
        journeyPattern.setRoute(route);
        List<StopPoint> stopPoints = new ArrayList<>();
        stopPoints.add(stopPoint1);
        stopPoints.add(stopPoint2);
        stopPoints.add(stopPoint3);
        journeyPattern.setStopPoints(stopPoints);
        route.setStopPoints(stopPoints);


        Timetable timetable = new Timetable();
        timetable.setObjectId("TEST:Timetable:t1");
        Period period = new Period();
        LocalDate startLocalDate = new LocalDate("2020-01-01");
        LocalDate endLocalDate = new LocalDate("2020-12-31");
        period.setStartDate(startLocalDate);
        period.setEndDate(endLocalDate);
        ArrayList<Period> periods = new ArrayList<>();
        periods.add(period);
        timetable.setPeriods(periods);

        List<Timetable> timetables = new ArrayList<>();
        timetables.add(timetable);

        HashSet<Timetable> timetableHashSet = new HashSet<>();
        timetableHashSet.add(timetable);


        VehicleJourneyAtStop vehicleJourneyAtStop1 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop1.setObjectId("TEST:TimetablePassingTime:tpt1");
        org.joda.time.LocalTime time1 = new org.joda.time.LocalTime(7, 0, 0);
        vehicleJourneyAtStop1.setDepartureTime(time1);
        vehicleJourneyAtStop1.setArrivalTime(time1);
        vehicleJourneyAtStop1.setStopPoint(stopPoint1);

        VehicleJourneyAtStop vehicleJourneyAtStop2 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop2.setObjectId("TEST:TimetablePassingTime:tpt2");
        org.joda.time.LocalTime time2 = new org.joda.time.LocalTime(7, 15, 0);
        vehicleJourneyAtStop2.setDepartureTime(time2);
        vehicleJourneyAtStop2.setArrivalTime(time2);
        vehicleJourneyAtStop2.setStopPoint(stopPoint2);

        VehicleJourneyAtStop vehicleJourneyAtStop3 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop3.setObjectId("TEST:TimetablePassingTime:tpt3");
        org.joda.time.LocalTime time3 = new org.joda.time.LocalTime(7, 30, 0);
        vehicleJourneyAtStop3.setDepartureTime(time3);
        vehicleJourneyAtStop3.setArrivalTime(time3);
        vehicleJourneyAtStop3.setStopPoint(stopPoint3);

        List<VehicleJourneyAtStop> vehicleJourneyAtStops = new ArrayList<>();
        vehicleJourneyAtStops.add(vehicleJourneyAtStop1);
        vehicleJourneyAtStops.add(vehicleJourneyAtStop2);
        vehicleJourneyAtStops.add(vehicleJourneyAtStop3);

        VehicleJourney vehicleJourney = new VehicleJourney();
        vehicleJourney.setObjectId("TEST:VehicleJourney:vj1");
        vehicleJourney.setPublishedJourneyName("Test vehicle journey name");
        vehicleJourney.setJourneyPattern(journeyPattern);
        vehicleJourney.setVehicleJourneyAtStops(vehicleJourneyAtStops);
        vehicleJourney.setTimetables(timetables);
        vehicleJourney.setRoute(route);

        ArrayList<Footnote> footnotes = new ArrayList<>();
        Footnote footnote = new Footnote();
        footnote.setObjectId("TEST:Footnote:f1");
        footnote.setLabel("Test Label");
        footnote.setCode("Test Code");
        footnotes.add(footnote);
        vehicleJourney.setFootnotes(footnotes);

        List<Route> routes = new ArrayList<>();
        routes.add(route);

        List<JourneyPattern> journeyPatterns = new ArrayList<>();
        journeyPatterns.add(journeyPattern);
        route.setJourneyPatterns(journeyPatterns);

        List<VehicleJourney> vehicleJourneys = new ArrayList<>();
        vehicleJourneys.add(vehicleJourney);

        Set<StopArea> stopAreas = new HashSet<>();
        stopAreas.add(stopArea1);
        stopAreas.add(stopArea2);
        stopAreas.add(stopArea3);


        Context context = new Context();
        JobDataTest jobData = new JobDataTest();
        jobData.setPathName(testPath);

        NetexprofileExportParameters parameters = new NetexprofileExportParameters();
        parameters.setExportStops(false);
        parameters.setAddMetadata(false);
        parameters.setDefaultCodespacePrefix("TEST");

        context.put(Constant.CONFIGURATION, parameters);
        context.put(JOB_DATA, jobData);
        context.put(REPORT, new ActionReport());
        NetexXMLProcessingHelperFactory netexXMLFactory = new NetexXMLProcessingHelperFactory();
        context.put(MARSHALLER, netexXMLFactory.createFragmentMarshaller());
        context.put(LINE,line);


        return context;
    }


}
