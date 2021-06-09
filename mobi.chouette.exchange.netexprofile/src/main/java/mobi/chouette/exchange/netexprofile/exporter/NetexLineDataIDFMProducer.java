package mobi.chouette.exchange.netexprofile.exporter;

import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.exchange.metadata.Metadata;
import mobi.chouette.exchange.metadata.NeptuneObjectPresenter;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.producer.CalendarIDFMProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.DirectionProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.LineFranceProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.LineProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetworkFranceProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.OrganisationFranceProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.OrganisationProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.RouteIDFMProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.RouteLinkProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.ServiceJourneyIDFMProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.ServiceJourneyPatternIDFMProducer;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Branding;
import mobi.chouette.model.Company;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Route;
import mobi.chouette.model.RouteSection;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.type.AlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingPossibilityEnum;
import org.rutebanken.netex.model.AvailabilityCondition;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.Organisation_VersionStructure;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ValidBetween;
import org.rutebanken.netex.model.ValidityConditions_RelStructure;
import org.rutebanken.netex.model.ValidityTriggerRefStructure;

import javax.xml.bind.Marshaller;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.netexId;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.PASSENGER_STOP_ASSIGNMENT;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.VALIDITY_CONDITIONS;

public class NetexLineDataIDFMProducer extends NetexProducer implements Constant {

    private static OrganisationFranceProducer organisationFranceProducer = new OrganisationFranceProducer();
    private static NetworkFranceProducer networkFranceProducer = new NetworkFranceProducer();
    private static LineFranceProducer lineFranceProducer = new LineFranceProducer();
    private static RouteIDFMProducer routeIDFMProducer = new RouteIDFMProducer();
    private static RouteLinkProducer routeLinkProducer = new RouteLinkProducer();
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

        deleteSpacesInIdsAndChangeSpecialCharacters(exportableData, parameters.getDefaultCodespacePrefix());

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

    /**
     * On remplace dans les object_id les espaces par rien, les caractères spéciaux par un _
     * On remplace le nom de l'espace de données qui constitue la première partie de l'object id par le defaultCodespacePrefix si il est différent (defaultCodespacePrefix vient du nameNetexOffreIdfm dans Okina/Baba)
     * @param exportableData
     * @param defaultCodespacePrefix
     */
    private void deleteSpacesInIdsAndChangeSpecialCharacters(ExportableData exportableData, String defaultCodespacePrefix) {
        for (Route route : exportableData.getRoutes()) {
            route.setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(route.getObjectId(), defaultCodespacePrefix));
            route.getLine().setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(route.getLine().getObjectId(), defaultCodespacePrefix));
            for (mobi.chouette.model.RoutePoint routePoint : route.getRoutePoints()) {
                routePoint.setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(routePoint.getObjectId(), defaultCodespacePrefix));
                routePoint.getScheduledStopPoint().setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(routePoint.getScheduledStopPoint().getObjectId(), defaultCodespacePrefix));
            }
        }
        for (JourneyPattern journeyPattern : exportableData.getJourneyPatterns()) {
            journeyPattern.setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(journeyPattern.getObjectId(), defaultCodespacePrefix));
            for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
                stopPoint.setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(stopPoint.getObjectId(), defaultCodespacePrefix));
                stopPoint.getScheduledStopPoint().setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(stopPoint.getScheduledStopPoint().getObjectId(), defaultCodespacePrefix));
                if (stopPoint.getDestinationDisplay() != null) {
                    stopPoint.getDestinationDisplay().setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(stopPoint.getDestinationDisplay().getObjectId(), defaultCodespacePrefix));
                }
            }

            for (RouteSection routeSection : journeyPattern.getRouteSections()){
                routeSection.setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(routeSection.getObjectId(),defaultCodespacePrefix));
            }
        }
        for (Timetable timetable : exportableData.getTimetables()) {
            timetable.setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(timetable.getObjectId(), defaultCodespacePrefix));
        }
        for (VehicleJourney vehicleJourney : exportableData.getVehicleJourneys()) {
            vehicleJourney.setObjectId(replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(vehicleJourney.getObjectId(), defaultCodespacePrefix));
        }
    }

    private String replaceAllSpacesAndSpecialCharacterAndReplaceNameDataSpace(String objectId, String defaultCodespacePrefix){
        objectId = objectId.replaceAll("\\s+", "");
        objectId = objectId.replaceAll(ID_STRUCTURE_REGEXP_SPECIAL_CHARACTER, "_");

        String[] nameDataSpace = objectId.split(":");
        if(!nameDataSpace[0].equals(defaultCodespacePrefix)){
            objectId = objectId.replace(nameDataSpace[0], defaultCodespacePrefix);
        }

        return objectId;
    }

    private void produceAndCollectLineData(Context context, ExportableData exportableData, ExportableNetexData exportableNetexData) {
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);

        mobi.chouette.model.Network neptuneNetwork = exportableData.getLine().getNetwork();
        org.rutebanken.netex.model.Network netexNetwork = exportableNetexData.getSharedNetworks().get(neptuneNetwork.getObjectId());

        if (netexNetwork == null) {
            netexNetwork = networkFranceProducer.produce(context, neptuneNetwork);
            exportableNetexData.getSharedNetworks().put(neptuneNetwork.getObjectId(), netexNetwork);
        }

        mobi.chouette.model.Line line = exportableData.getLine();
        boolean allSPTad = exportableData.getStopPoints().stream().allMatch(sp -> AlightingPossibilityEnum.is_flexible.equals(sp.getForAlighting()) && BoardingPossibilityEnum.is_flexible.equals(sp.getForBoarding()));
        if(allSPTad){
            line.setFlexibleService(true);
        }
        org.rutebanken.netex.model.Line_VersionStructure netexLine = lineFranceProducer.produce(context, line);
        exportableNetexData.setLine(netexLine);

        for (Company company : exportableData.getCompanies()) {
            if (!exportableNetexData.getSharedOrganisations().containsKey(company.getObjectId())) {
                Organisation_VersionStructure organisation = organisationFranceProducer.produce(context, company);
                exportableNetexData.getSharedOrganisations().put(company.getObjectId(), organisation);
            }
        }

        for (mobi.chouette.model.Route neptuneRoute : exportableData.getRoutes()) {
            exportableNetexData.getRoutes().add(routeIDFMProducer.produce(context, neptuneRoute));
        }

        producerAndCollectDirection(exportableData.getRoutes(), exportableNetexData);

        for (JourneyPattern neptuneJourneyPattern : exportableData.getJourneyPatterns()) {
            exportableNetexData.getServiceJourneyPatterns().add(serviceJourneyPatternIDFMProducer.produce(neptuneJourneyPattern));

            for (RouteSection routeSection : neptuneJourneyPattern.getRouteSections()) {
                org.rutebanken.netex.model.RouteLink routeLink = routeLinkProducer.produce(context, routeSection);
                exportableNetexData.getRouteLinks().add(routeLink);
            }
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


                LocationStructure locationStructure = new LocationStructure();
                locationStructure.setLatitude(chouetteScheduledStopPoint.getContainedInStopAreaRef().getObject().getLatitude());

                locationStructure.setLongitude(chouetteScheduledStopPoint.getContainedInStopAreaRef().getObject().getLongitude());

                scheduledStopPoint.setLocation(locationStructure);
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

            quayRefStruct.setValue("version=\"any\"");

            passengerStopAssignment.setQuayRef(quayRefStruct);
        }

        passengerStopAssignment.setId(passengerStopAssignment.getId() + ":LOC");
        passengerStopAssignment.setVersion("any");

        return passengerStopAssignment;
    }

}
