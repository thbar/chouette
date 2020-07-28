package mobi.chouette.exchange.netexprofile.exporter;

import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.exchange.metadata.Metadata;
import mobi.chouette.exchange.metadata.NeptuneObjectPresenter;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.producer.CalendarIDFMProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.DirectionProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import mobi.chouette.exchange.netexprofile.exporter.producer.RouteIDFMProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.ServiceJourneyIDFMProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.ServiceJourneyPatternIDFMProducer;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;

import javax.xml.bind.Marshaller;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.netexId;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.PASSENGER_STOP_ASSIGNMENT;

public class NetexLineDataIDFMProducer extends NetexProducer implements Constant {

    private static RouteIDFMProducer routeIDFMProducer = new RouteIDFMProducer();
    private static CalendarIDFMProducer calendarIDFMProducer = new CalendarIDFMProducer();
    private static ServiceJourneyIDFMProducer serviceJourneyIDFMProducer = new ServiceJourneyIDFMProducer();
    private static DirectionProducer directionProducer = new DirectionProducer();
    private static ServiceJourneyPatternIDFMProducer serviceJourneyPatternIDFMProducer = new ServiceJourneyPatternIDFMProducer();

    protected static final String ID_STRUCTURE_REGEXP_SPECIAL_CHARACTER = "([^0-9A-Za-z-_:])";


    public void produce(Context context) throws Exception {

        NetexprofileExportParameters parameters = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);

        ActionReporter reporter = ActionReporter.Factory.getInstance();
        JobData jobData = (JobData) context.get(JOB_DATA);
        Path outputPath = Paths.get(jobData.getPathName(), OUTPUT);
        ExportableData exportableData = (ExportableData) context.get(EXPORTABLE_DATA);
        ExportableNetexData exportableNetexData = (ExportableNetexData) context.get(EXPORTABLE_NETEX_DATA);
        mobi.chouette.model.Line neptuneLine = exportableData.getLine();

        deleteSpacesInIdsAndChangeSpecialCharacters(exportableData);

        // Pour info il n'y a pas de produceAndCollectCommonData car les notices utilisés pour créer ce fichier sont récupérés dans les deux méthodes ci dessous
        produceAndCollectLineData(context, exportableData, exportableNetexData);
        produceAndCollectCalendarData(exportableData, exportableNetexData);

        String fileName = ExportedFilenamer.createIDFMLineFilename(context, neptuneLine);
        reporter.addFileReport(context, fileName, IO_TYPE.OUTPUT);
        Path filePath = new File(outputPath.toFile(), fileName).toPath();

        Marshaller marshaller = (Marshaller) context.get(MARSHALLER);
        NetexFileWriter writer = new NetexFileWriter();
        writer.writeXmlFile(context, filePath, exportableData, exportableNetexData, NetexFragmentMode.LINE, marshaller);

        if (parameters.isAddMetadata()) {
            Metadata metadata = (Metadata) context.get(METADATA);
            if (metadata != null) {
                metadata.getResources().add(
                        metadata.new Resource(fileName, NeptuneObjectPresenter.getName(neptuneLine.getNetwork()), NeptuneObjectPresenter.getName(neptuneLine)));
            }
        }
    }

    private void produceAndCollectCalendarData(ExportableData exportableData, ExportableNetexData exportableNetexData) {
        calendarIDFMProducer.produce(exportableData, exportableNetexData);
    }

    private void deleteSpacesInIdsAndChangeSpecialCharacters(ExportableData exportableData) {
        for (Route route : exportableData.getRoutes()) {
            route.setObjectId(replaceAllSpacesAndSpecialCharacter(route.getObjectId()));
            route.getLine().setObjectId(replaceAllSpacesAndSpecialCharacter(route.getLine().getObjectId()));
            for (mobi.chouette.model.RoutePoint routePoint : route.getRoutePoints()) {
                routePoint.setObjectId(replaceAllSpacesAndSpecialCharacter(routePoint.getObjectId()));
                routePoint.getScheduledStopPoint().setObjectId(replaceAllSpacesAndSpecialCharacter(routePoint.getScheduledStopPoint().getObjectId()));
            }
        }
        for (JourneyPattern journeyPattern : exportableData.getJourneyPatterns()) {
            journeyPattern.setObjectId(replaceAllSpacesAndSpecialCharacter(journeyPattern.getObjectId()));
            for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
                stopPoint.setObjectId(replaceAllSpacesAndSpecialCharacter(stopPoint.getObjectId()));
                stopPoint.getScheduledStopPoint().setObjectId(replaceAllSpacesAndSpecialCharacter(stopPoint.getScheduledStopPoint().getObjectId()));
                if (stopPoint.getDestinationDisplay() != null) {
                    stopPoint.getDestinationDisplay().setObjectId(replaceAllSpacesAndSpecialCharacter(stopPoint.getDestinationDisplay().getObjectId()));
                }
            }
        }
        for (Timetable timetable : exportableData.getTimetables()) {
            timetable.setObjectId(replaceAllSpacesAndSpecialCharacter(timetable.getObjectId()));
        }
        for (VehicleJourney vehicleJourney : exportableData.getVehicleJourneys()) {
            vehicleJourney.setObjectId(replaceAllSpacesAndSpecialCharacter(vehicleJourney.getObjectId()));
        }
    }

    private String replaceAllSpacesAndSpecialCharacter(String objectId){
        objectId = objectId.replaceAll("\\s+", "");
        objectId = objectId.replaceAll(ID_STRUCTURE_REGEXP_SPECIAL_CHARACTER, "_");

        return objectId;
    }

    private void produceAndCollectLineData(Context context, ExportableData exportableData, ExportableNetexData exportableNetexData) {
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);

        for (mobi.chouette.model.Route neptuneRoute : exportableData.getRoutes()) {
            exportableNetexData.getRoutes().add(routeIDFMProducer.produce(context, neptuneRoute));
        }

        producerAndCollectDirection(exportableData.getRoutes(), exportableNetexData);

        for (JourneyPattern neptuneJourneyPattern : exportableData.getJourneyPatterns()) {
            exportableNetexData.getServiceJourneyPatterns().add(serviceJourneyPatternIDFMProducer.produce(neptuneJourneyPattern));
        }

        produceAndCollectScheduledStopPoints(exportableData.getRoutes(), exportableNetexData);

        produceAndCollectPassengerStopAssignments(exportableData.getRoutes(), exportableNetexData, configuration);

        List<Route> activeRoutes = exportableData.getVehicleJourneys().stream().map(vj -> vj.getRoute()).distinct().collect(Collectors.toList());
        produceAndCollectDestinationDisplays(activeRoutes, exportableNetexData);

        for (mobi.chouette.model.VehicleJourney vehicleJourney : exportableData.getVehicleJourneys()) {
            exportableNetexData.getServiceJourneys().add(serviceJourneyIDFMProducer.produce(context, vehicleJourney));
        }
    }

    private void producerAndCollectDirection(List<Route> routes, ExportableNetexData exportableNetexData) {
        for (Route route : routes) {
            for (StopPoint stopPoint : route.getStopPoints()) {
                if (stopPoint.getPosition().equals(route.getStopPoints().size() - 1)) { ;
                    exportableNetexData.getDirections().add(directionProducer.produce(stopPoint));
                }
            }
        }
    }

    private void produceAndCollectScheduledStopPoints(List<mobi.chouette.model.Route> routes, ExportableNetexData exportableNetexData) {
        for (mobi.chouette.model.Route route : routes) {
            for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
                for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
                    if (stopPoint != null) {
                        collectScheduledStopPoint(stopPoint.getScheduledStopPoint(), exportableNetexData);
                    }
                }
            }
            for (mobi.chouette.model.RoutePoint routePoint : route.getRoutePoints()) {
                collectScheduledStopPoint(routePoint.getScheduledStopPoint(), exportableNetexData);
            }
        }
    }

    public void collectScheduledStopPoint(mobi.chouette.model.ScheduledStopPoint chouetteScheduledStopPoint, ExportableNetexData exportableNetexData) {
        if (chouetteScheduledStopPoint != null) {
            if (isSet(chouetteScheduledStopPoint.getContainedInStopAreaRef().getObject())) {
                ScheduledStopPoint scheduledStopPoint = netexFactory.createScheduledStopPoint();
                NetexProducerUtils.populateIdAndVersionIDFM(chouetteScheduledStopPoint, scheduledStopPoint);
                exportableNetexData.getScheduledStopPoints().put(scheduledStopPoint.getId(), scheduledStopPoint);
            } else {
                throw new RuntimeException(
                        "ScheduledStopPoint with id : " + chouetteScheduledStopPoint.getObjectId() + " is not contained in a StopArea. Cannot produce ScheduledStopPoint.");
            }
        }
    }

    private void produceAndCollectDestinationDisplays(List<mobi.chouette.model.Route> routes, ExportableNetexData exportableNetexData) {
        for (mobi.chouette.model.Route route : routes) {
            for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
                for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
                    if (stopPoint != null) {
                        mobi.chouette.model.DestinationDisplay dd = stopPoint.getDestinationDisplay();
                        if (dd != null) {
                            addDestinationDisplay(dd, exportableNetexData);
                        }
                    }
                }
            }
        }
    }

    protected void addDestinationDisplay(mobi.chouette.model.DestinationDisplay dd, ExportableNetexData exportableNetexData) {
            DestinationDisplay netexDestinationDisplay = netexFactory.createDestinationDisplay();
            NetexProducerUtils.populateIdAndVersionIDFM(dd, netexDestinationDisplay);
            netexDestinationDisplay.setFrontText(ConversionUtil.getMultiLingualString(dd.getFrontText()));
            exportableNetexData.getDestinationDisplays().put(netexDestinationDisplay.getId(), netexDestinationDisplay);
    }

    private void produceAndCollectPassengerStopAssignments(List<mobi.chouette.model.Route> routes, ExportableNetexData exportableNetexData,
                                                           NetexprofileExportParameters parameters) {
        for (mobi.chouette.model.Route route : routes) {
            for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
                for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
                    if (stopPoint != null) {
                        collectPassengerStopAssignment(exportableNetexData, parameters, stopPoint.getScheduledStopPoint());
                    }
                }
            }
            for (mobi.chouette.model.RoutePoint routePoint : route.getRoutePoints()) {
                if (routePoint.getScheduledStopPoint() != null) {
                    collectPassengerStopAssignment(exportableNetexData, parameters, routePoint.getScheduledStopPoint());
                }
            }
        }
    }

    private void collectPassengerStopAssignment(ExportableNetexData exportableNetexData, NetexprofileExportParameters parameters, mobi.chouette.model.ScheduledStopPoint scheduledStopPoint) {
        if (isSet(scheduledStopPoint)) {
            String passengerStopAssignmentIdSuffix = scheduledStopPoint.objectIdSuffix();
            String passengerStopAssignmentId = netexId(scheduledStopPoint.objectIdPrefix(), PASSENGER_STOP_ASSIGNMENT, passengerStopAssignmentIdSuffix);
            PassengerStopAssignment stopAssignment = createPassengerStopAssignment(scheduledStopPoint, passengerStopAssignmentId, parameters);
            exportableNetexData.getStopAssignments().put(stopAssignment.getId(), stopAssignment);
        } else {
            throw new RuntimeException(
                    "ScheduledStopPoint with id : " + scheduledStopPoint.getObjectId() + " is not contained in a StopArea. Cannot produce StopAssignment.");
        }
    }

    private PassengerStopAssignment createPassengerStopAssignment(mobi.chouette.model.ScheduledStopPoint scheduledStopPoint, String stopAssignmentId, NetexprofileExportParameters parameters) {
        PassengerStopAssignment passengerStopAssignment = netexFactory.createPassengerStopAssignment().withVersion(NETEX_DEFAULT_OBJECT_VERSION).withId(stopAssignmentId)
                .withOrder(BigInteger.valueOf(0));

        ScheduledStopPointRefStructure scheduledStopPointRef = netexFactory.createScheduledStopPointRefStructure();
        NetexProducerUtils.populateReferenceIDFM(scheduledStopPoint, scheduledStopPointRef);

        passengerStopAssignment.setScheduledStopPointRef(netexFactory.createScheduledStopPointRef(scheduledStopPointRef));

        if (isSet(scheduledStopPoint.getContainedInStopAreaRef().getObject())) {
            mobi.chouette.model.StopArea containedInStopArea = scheduledStopPoint.getContainedInStopAreaRef().getObject();
            QuayRefStructure quayRefStruct = netexFactory.createQuayRefStructure();
            NetexProducerUtils.populateReference(containedInStopArea, quayRefStruct, parameters.isExportStops());

            if(containedInStopArea.getMappingHastusZdep() != null && containedInStopArea.getMappingHastusZdep().getZdep() != null){
                quayRefStruct.setRef("FR::Quay:" + containedInStopArea.getMappingHastusZdep().getZdep() + ":FR1");
            }
            else{
                //TODO voir le traitement à faire
            }
            quayRefStruct.setValue("version=\"any\"");

            passengerStopAssignment.setQuayRef(quayRefStruct);
        }

        passengerStopAssignment.setId(passengerStopAssignment.getId() + ":LOC");
        passengerStopAssignment.setVersion("any");

        return passengerStopAssignment;
    }

}
