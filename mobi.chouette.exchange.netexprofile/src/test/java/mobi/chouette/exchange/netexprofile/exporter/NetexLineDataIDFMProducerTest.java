package mobi.chouette.exchange.netexprofile.exporter;


import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.JobDataTest;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.model.DestinationDisplay;
import mobi.chouette.model.Footnote;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.model.Period;
import mobi.chouette.model.Route;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.SimpleObjectReference;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.PTDirectionEnum;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.rutebanken.netex.model.DayTypeAssignment_VersionStructure;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.NoticeAssignmentRefStructure;
import org.rutebanken.netex.model.NoticeAssignments_RelStructure;
import org.rutebanken.netex.model.OperatingPeriodRefStructure;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static mobi.chouette.common.Constant.EXPORTABLE_DATA;
import static mobi.chouette.common.Constant.JOB_DATA;
import static mobi.chouette.common.Constant.REPORT;
import static mobi.chouette.exchange.netexprofile.Constant.EXPORTABLE_NETEX_DATA;
import static mobi.chouette.exchange.netexprofile.Constant.MARSHALLER;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;

public class NetexLineDataIDFMProducerTest {

    @Test
    public void exportOffreIDFM() throws Exception {

        Context context = createContext();

        NetexLineDataIDFMProducer netexLineDataIDFMProducer = new NetexLineDataIDFMProducer();
        netexLineDataIDFMProducer.produce(context);

        ExportableNetexData exportableNetexDataResult = (ExportableNetexData) context.get(EXPORTABLE_NETEX_DATA);

        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getId(), "TEST:Route:r1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getLineRef().getValue().getRef(), "TEST:Line:TestCodifligne:LOC");
        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getDirectionRef().getRef(), "TEST:Direction:r1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getDirectionRef().getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getDirections().get(0).getId(),"TEST:Direction:r1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getDirections().get(0).getId(), exportableNetexDataResult.getRoutes().get(0).getDirectionRef().getRef());
        Assert.assertEquals(exportableNetexDataResult.getDirections().get(0).getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getId(), "TEST:ServiceJourneyPattern:jp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getName().getValue(), "Test Journey Pattern");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getRouteRef().getRef(), "TEST:Route:r1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getRouteRef().getRef(), exportableNetexDataResult.getRoutes().get(0).getId());
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getRouteRef().getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getDestinationDisplayRef().getRef(), "TEST:DestinationDisplay:dd1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getDestinationDisplayRef().getRef(), exportableNetexDataResult.getSharedDestinationDisplays().get("TEST:DestinationDisplay:dd1:LOC").getId());
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getDestinationDisplayRef().getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(0).getId(), "TEST:StopPointInJourneyPattern:sp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(0).getOrder(), BigInteger.valueOf(1));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(0).getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(1).getId(), "TEST:StopPointInJourneyPattern:sp2:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(1).getOrder(), BigInteger.valueOf(2));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(1).getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(2).getId(), "TEST:StopPointInJourneyPattern:sp3:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(2).getOrder(), BigInteger.valueOf(3));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(2).getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getSharedScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp1:LOC").getId(), "TEST:ScheduledStopPoint:ssp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp2:LOC").getId(), "TEST:ScheduledStopPoint:ssp2:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp3:LOC").getId(), "TEST:ScheduledStopPoint:ssp3:LOC");

        Assert.assertEquals(exportableNetexDataResult.getSharedScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp1:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getSharedScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp2:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getSharedScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp3:LOC").getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getId(), "TEST:PassengerStopAssignment:ssp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getId(), "TEST:PassengerStopAssignment:ssp2:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getId(), "TEST:PassengerStopAssignment:ssp3:LOC");

        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getOrder(), BigInteger.valueOf(0));
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getOrder(), BigInteger.valueOf(0));
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getOrder(), BigInteger.valueOf(0));

        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getId(), "TEST:PassengerStopAssignment:ssp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getId(), "TEST:PassengerStopAssignment:ssp2:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getId(), "TEST:PassengerStopAssignment:ssp3:LOC");

        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getScheduledStopPointRef().getValue().getRef(), "TEST:ScheduledStopPoint:ssp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getScheduledStopPointRef().getValue().getRef(), "TEST:ScheduledStopPoint:ssp2:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getScheduledStopPointRef().getValue().getRef(), "TEST:ScheduledStopPoint:ssp3:LOC");

        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getScheduledStopPointRef().getValue().getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getScheduledStopPointRef().getValue().getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getScheduledStopPointRef().getValue().getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getQuayRef().getRef(), "FR::Quay:testzdep1:FR1");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getQuayRef().getRef(), "FR::Quay:testzdep2:FR1");
        Assert.assertEquals(exportableNetexDataResult.getSharedStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getQuayRef().getRef(), "FR::Quay:testzdep3:FR1");

        Assert.assertEquals(exportableNetexDataResult.getSharedDestinationDisplays().get("TEST:DestinationDisplay:dd1:LOC").getId(), "TEST:DestinationDisplay:dd1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedDestinationDisplays().get("TEST:DestinationDisplay:dd1:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getSharedDestinationDisplays().get("TEST:DestinationDisplay:dd1:LOC").getFrontText().getValue(), "Test Destination Display");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getId(), "TEST:ServiceJourney:vj1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getName().getValue(), "Test vehicle journey name");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getJourneyPatternRef().getValue().getRef(), "TEST:ServiceJourneyPattern:jp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getJourneyPatternRef().getValue().getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getDayTypes().getDayTypeRef().get(0).getValue().getRef(), "TEST:DayType:t1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getDayTypes().getDayTypeRef().get(0).getValue().getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(0).getDepartureTime(), TimeUtil.toLocalTimeFromJoda(new LocalTime(7, 0, 0)));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(0).getVersion(), "any");
        Assert.assertNull(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(0).getArrivalTime());

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(1).getDepartureTime(), TimeUtil.toLocalTimeFromJoda(new LocalTime(7, 15, 0)));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(1).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(1).getArrivalTime(), TimeUtil.toLocalTimeFromJoda(new LocalTime(7, 15, 0)));

        Assert.assertNull(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(2).getDepartureTime());
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(2).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(2).getArrivalTime(), TimeUtil.toLocalTimeFromJoda(new LocalTime(7, 30, 0)));

        Assert.assertEquals(exportableNetexDataResult.getSharedDayTypes().get("TEST:DayType:t1:LOC").getId(), "TEST:DayType:t1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedDayTypes().get("TEST:DayType:t1:LOC").getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getSharedOperatingPeriods().get("TEST:OperatingPeriod:t1:LOC").getId(), "TEST:OperatingPeriod:t1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedOperatingPeriods().get("TEST:OperatingPeriod:t1:LOC").getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getSharedOperatingPeriods().get("TEST:OperatingPeriod:t1:LOC").getFromDate(), LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        Assert.assertEquals(exportableNetexDataResult.getSharedOperatingPeriods().get("TEST:OperatingPeriod:t1:LOC").getToDate(), LocalDateTime.of(2020, 12, 31, 0, 0, 0));

        List<JAXBElement<? extends DayTypeRefStructure>> dayTypesRef = exportableNetexDataResult.getSharedDayTypeAssignments().stream().map(DayTypeAssignment_VersionStructure::getDayTypeRef).collect(Collectors.toList());
        Assert.assertEquals(dayTypesRef.get(0).getValue().getRef(), "TEST:DayType:t1:LOC");
        Assert.assertEquals(dayTypesRef.get(0).getValue().getVersion(), "any");

        List<OperatingPeriodRefStructure> operatingPeriodsRef = exportableNetexDataResult.getSharedDayTypeAssignments().stream().map(DayTypeAssignment_VersionStructure::getOperatingPeriodRef).collect(Collectors.toList());
        Assert.assertEquals(operatingPeriodsRef.get(0).getRef(), "TEST:OperatingPeriod:t1:LOC");
        Assert.assertEquals(operatingPeriodsRef.get(0).getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getId(), "TEST:Notice:f1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getTypeOfNoticeRef().getRef(), "ServiceJourneyNotice");
        Assert.assertEquals(exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getPublicCode(), "Test Code");
        Assert.assertEquals(exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getText().getValue(), "Test Label");

        NoticeAssignment noticeAssignment =
                (NoticeAssignment) exportableNetexDataResult.getServiceJourneys().get(0).getNoticeAssignments().getNoticeAssignment_OrNoticeAssignmentView().get(0).getValue();

        Assert.assertEquals(noticeAssignment.getId(), "TEST:NoticeAssignment:1:LOC");
        Assert.assertEquals(noticeAssignment.getVersion(), "any");
        Assert.assertEquals(noticeAssignment.getOrder(), BigInteger.valueOf(0));
        Assert.assertEquals(noticeAssignment.getNoticeRef().getRef(), "TEST:Notice:f1:LOC");
        Assert.assertEquals(noticeAssignment.getNoticeRef().getRef(), exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getId());
        Assert.assertEquals(noticeAssignment.getNoticeRef().getValue(), "version=\"any\"");

        deleteFileCreated();

    }

    private void deleteFileCreated() {
        File file = new File("src/test/data/idfm/output/offre_TestCodifligne_.xml");
        if(file.delete()){
            System.out.println("Fichier de test supprimé");
        }else{
            System.out.println("ERREUR fichier de test non supprimé");
        }
    }

    private Context createContext() throws JAXBException {
        Line line = new Line();
        line.setObjectId("TEST:Line:l1");
        line.setRegistrationNumber("l1");
        line.setCodifligne("TestCodifligne");

        Route route  = new Route();
        route.setObjectId("TEST:Route:r1");
        route.setName("Test Route");
        route.setLine(line);
        route.setDirection(PTDirectionEnum.A);

        DestinationDisplay destinationDisplay = new DestinationDisplay();
        destinationDisplay.setObjectId("TEST:DestinationDisplay:dd1");
        destinationDisplay.setFrontText("Test Destination Display");

        MappingHastusZdep mappingHastusZdep1 = new MappingHastusZdep();
        mappingHastusZdep1.setHastusChouette("testquay1");
        mappingHastusZdep1.setZdep("testzdep1");

        MappingHastusZdep mappingHastusZdep2 = new MappingHastusZdep();
        mappingHastusZdep2.setHastusChouette("testquay2");
        mappingHastusZdep2.setZdep("testzdep2");

        MappingHastusZdep mappingHastusZdep3 = new MappingHastusZdep();
        mappingHastusZdep3.setHastusChouette("testquay3");
        mappingHastusZdep3.setZdep("testzdep3");

        StopArea stopArea1 = new StopArea();
        stopArea1.setObjectId("TEST:Quay:quay1");
        stopArea1.setMappingHastusZdep(mappingHastusZdep1);

        StopArea stopArea2 = new StopArea();
        stopArea2.setObjectId("TEST:Quay:quay2");
        stopArea2.setMappingHastusZdep(mappingHastusZdep2);

        StopArea stopArea3 = new StopArea();
        stopArea3.setObjectId("TEST:Quay:quay3");
        stopArea3.setMappingHastusZdep(mappingHastusZdep3);

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
        LocalTime time1 = new LocalTime(7, 0, 0);
        vehicleJourneyAtStop1.setDepartureTime(time1);
        vehicleJourneyAtStop1.setArrivalTime(time1);
        vehicleJourneyAtStop1.setStopPoint(stopPoint1);

        VehicleJourneyAtStop vehicleJourneyAtStop2 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop2.setObjectId("TEST:TimetablePassingTime:tpt2");
        LocalTime time2 = new LocalTime(7, 15, 0);
        vehicleJourneyAtStop2.setDepartureTime(time2);
        vehicleJourneyAtStop2.setArrivalTime(time2);
        vehicleJourneyAtStop2.setStopPoint(stopPoint2);

        VehicleJourneyAtStop vehicleJourneyAtStop3 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop3.setObjectId("TEST:TimetablePassingTime:tpt3");
        LocalTime time3 = new LocalTime(7, 30, 0);
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


        ExportableData exportableData = new ExportableData();
        ExportableNetexData exportableNetexData = new ExportableNetexData();


        exportableData.setLine(line);
        exportableData.setRoutes(routes);
        exportableData.setJourneyPatterns(journeyPatterns);
        exportableData.setVehicleJourneys(vehicleJourneys);
        exportableData.setStopAreas(stopAreas);
        exportableData.setTimetables(timetableHashSet);

        Context context = new Context();
        JobDataTest jobData = new JobDataTest();
        jobData.setPathName("src/test/data/idfm");

        NetexprofileExportParameters parameters = new NetexprofileExportParameters();
        parameters.setExportStops(false);
        parameters.setAddMetadata(false);
        parameters.setDefaultCodespacePrefix("TEST");

        context.put(Constant.CONFIGURATION, parameters);
        context.put(EXPORTABLE_DATA, exportableData);
        context.put(EXPORTABLE_NETEX_DATA, exportableNetexData);
        context.put(JOB_DATA, jobData);
        context.put(REPORT, new ActionReport());
        NetexXMLProcessingHelperFactory netexXMLFactory = new NetexXMLProcessingHelperFactory();
        context.put(MARSHALLER, netexXMLFactory.createFragmentMarshaller());


        return context;
    }
}
