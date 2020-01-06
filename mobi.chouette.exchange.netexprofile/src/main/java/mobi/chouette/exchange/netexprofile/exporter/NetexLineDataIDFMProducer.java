package mobi.chouette.exchange.netexprofile.exporter;

import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.exchange.metadata.Metadata;
import mobi.chouette.exchange.metadata.NeptuneObjectPresenter;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.producer.BrandingProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.CalendarIDFMProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.DirectionProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.LineProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetworkProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.OrganisationProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.RouteIDFMProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.ServiceJourneyIDFMProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.ServiceJourneyInterchangeProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.ServiceJourneyPatternProducer;
import mobi.chouette.exchange.netexprofile.exporter.producer.StopPlaceProducer;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Branding;
import mobi.chouette.model.Company;
import mobi.chouette.model.GroupOfLine;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import org.apache.commons.collections.CollectionUtils;
import org.rutebanken.netex.model.AvailabilityCondition;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.Direction;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.Organisation_VersionStructure;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPlace;

import javax.xml.bind.Marshaller;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.createUniqueId;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.netexId;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.PASSENGER_STOP_ASSIGNMENT;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.POINT_PROJECTION;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.SCHEDULED_STOP_POINT;

public class NetexLineDataIDFMProducer extends NetexProducer implements Constant {

    private static OrganisationProducer organisationProducer = new OrganisationProducer();
    private static StopPlaceProducer stopPlaceProducer = new StopPlaceProducer();
    private static LineProducer lineProducer = new LineProducer();
    private static RouteIDFMProducer routeIDFMProducer = new RouteIDFMProducer();
    private static CalendarIDFMProducer calendarIDFMProducer = new CalendarIDFMProducer();
    private static ServiceJourneyIDFMProducer serviceJourneyIDFMProducer = new ServiceJourneyIDFMProducer();
    private static ServiceJourneyInterchangeProducer serviceJourneyInterchangeProducer = new ServiceJourneyInterchangeProducer();
    private static BrandingProducer brandingProducer = new BrandingProducer();
    private static DirectionProducer directionProducer = new DirectionProducer();
    private static ServiceJourneyPatternProducer serviceJourneyPatternProducer = new ServiceJourneyPatternProducer();

    public void produce(Context context) throws Exception {

        NetexprofileExportParameters parameters = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);

        ActionReporter reporter = ActionReporter.Factory.getInstance();
        JobData jobData = (JobData) context.get(JOB_DATA);
        Path outputPath = Paths.get(jobData.getPathName(), OUTPUT);
        ExportableData exportableData = (ExportableData) context.get(EXPORTABLE_DATA);
        ExportableNetexData exportableNetexData = (ExportableNetexData) context.get(EXPORTABLE_NETEX_DATA);
        mobi.chouette.model.Line neptuneLine = exportableData.getLine();

        deleteSpacesInIds(exportableData);

        produceAndCollectLineData(context, exportableData, exportableNetexData);
        produceAndCollectSharedData(context, exportableData, exportableNetexData);

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

    private void deleteSpacesInIds(ExportableData exportableData) {
        for (Route route : exportableData.getRoutes()) {
            route.setObjectId(route.getObjectId().replaceAll("\\s+", ""));
            route.getLine().setObjectId(route.getLine().getObjectId().replaceAll("\\s+", ""));
            for (mobi.chouette.model.RoutePoint routePoint : route.getRoutePoints()) {
                routePoint.setObjectId(routePoint.getObjectId().replaceAll("\\s+", ""));
                routePoint.getScheduledStopPoint().setObjectId(routePoint.getScheduledStopPoint().getObjectId().replaceAll("\\s+", ""));
            }
        }
        for (JourneyPattern journeyPattern : exportableData.getJourneyPatterns()) {
            journeyPattern.setObjectId(journeyPattern.getObjectId().replaceAll("\\s+", ""));
            for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
                stopPoint.setObjectId(stopPoint.getObjectId().replaceAll("\\s+", ""));
                stopPoint.getScheduledStopPoint().setObjectId(stopPoint.getScheduledStopPoint().getObjectId().replaceAll("\\s+", ""));
                if (stopPoint.getDestinationDisplay() != null) {
                    stopPoint.getDestinationDisplay().setObjectId(stopPoint.getDestinationDisplay().getObjectId().replaceAll("\\s+", ""));
                }
            }
        }
        for (Timetable timetable : exportableData.getTimetables()) {
            timetable.setObjectId(timetable.getObjectId().replaceAll("\\s+", ""));
        }
        for (VehicleJourney vehicleJourney : exportableData.getVehicleJourneys()) {
            vehicleJourney.setObjectId(vehicleJourney.getObjectId().replaceAll("\\s+", ""));
        }
    }

    private void produceAndCollectLineData(Context context, ExportableData exportableData, ExportableNetexData exportableNetexData) {
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);

        mobi.chouette.model.Line neptuneLine = exportableData.getLine();

        AvailabilityCondition availabilityCondition = createAvailabilityCondition(context);
        exportableNetexData.setLineCondition(availabilityCondition);

        org.rutebanken.netex.model.Line_VersionStructure netexLine = lineProducer.produce(context, neptuneLine);
        exportableNetexData.setLine(netexLine);

        for (mobi.chouette.model.Route neptuneRoute : exportableData.getRoutes()) {
            org.rutebanken.netex.model.Route netexRoute = routeIDFMProducer.produce(context, neptuneRoute);
            exportableNetexData.getRoutes().add(netexRoute);
        }

        producerAndCollectDirection(exportableData.getRoutes(), exportableNetexData);

        for (JourneyPattern neptuneJourneyPattern : exportableData.getJourneyPatterns()) {
            org.rutebanken.netex.model.ServiceJourneyPattern netexServiceJourneyPattern = serviceJourneyPatternProducer.produce(neptuneJourneyPattern);
            exportableNetexData.getServiceJourneyPattern().add(netexServiceJourneyPattern);
        }

        produceAndCollectScheduledStopPoints(exportableData.getRoutes(), exportableNetexData);

        produceAndCollectPassengerStopAssignments(exportableData.getRoutes(), exportableNetexData, configuration);

        calendarIDFMProducer.produce(context, exportableData, exportableNetexData);

        for (mobi.chouette.model.VehicleJourney vehicleJourney : exportableData.getVehicleJourneys()) {
            ServiceJourney serviceJourney = serviceJourneyIDFMProducer.produce(context, vehicleJourney, exportableData.getLine());
            exportableNetexData.getServiceJourneys().add(serviceJourney);
        }
    }

    private void producerAndCollectDirection(List<Route> routes, ExportableNetexData exportableNetexData) {
        for (Route route : routes) {
            for (StopPoint stopPoint : route.getStopPoints()) {
                if (stopPoint.getPosition().equals(route.getStopPoints().size() - 1)) {
                    Direction direction = directionProducer.produce(stopPoint);
                    exportableNetexData.getDirections().add(direction);
                }
            }
        }
    }

    private void produceAndCollectSharedData(Context context, ExportableData exportableData, ExportableNetexData exportableNetexData) {
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);

        produceAndCollectCodespaces(context, exportableNetexData);

        mobi.chouette.model.Network neptuneNetwork = exportableData.getLine().getNetwork();
        org.rutebanken.netex.model.Network netexNetwork = exportableNetexData.getSharedNetworks().get(neptuneNetwork.getObjectId());

        if (CollectionUtils.isNotEmpty(exportableData.getLine().getGroupOfLines())) {
            GroupOfLine groupOfLine = exportableData.getLine().getGroupOfLines().get(0);
            GroupOfLines groupOfLines = exportableNetexData.getSharedGroupsOfLines().get(groupOfLine.getObjectId());

            if (groupOfLines == null) {
                groupOfLines = createGroupOfLines(groupOfLine);
                exportableNetexData.getSharedGroupsOfLines().put(groupOfLine.getObjectId(), groupOfLines);
            }
            if (netexNetwork.getGroupsOfLines() == null) {
                netexNetwork.setGroupsOfLines(netexFactory.createGroupsOfLinesInFrame_RelStructure());
            }
            if (!netexNetwork.getGroupsOfLines().getGroupOfLines().contains(groupOfLines)) {
                netexNetwork.getGroupsOfLines().getGroupOfLines().add(groupOfLines);
            }
        }

        AvailabilityCondition availabilityCondition = createAvailabilityCondition(context);
        exportableNetexData.setCommonCondition(availabilityCondition);

        for (Company company : exportableData.getCompanies()) {
            if (!exportableNetexData.getSharedOrganisations().containsKey(company.getObjectId())) {
                Organisation_VersionStructure organisation = organisationProducer.produce(context, company);
                exportableNetexData.getSharedOrganisations().put(company.getObjectId(), organisation);

                Branding branding = company.getBranding();
                if (branding != null && !exportableNetexData.getSharedBrandings().containsKey(branding.getObjectId())) {
                    brandingProducer.addBranding(exportableNetexData, branding);
                }
            }
        }

        if (configuration.isExportStops()) {
            Set<StopArea> stopAreas = new HashSet<>();
            stopAreas.addAll(exportableData.getStopPlaces());
            stopAreas.addAll(exportableData.getCommercialStops());

            for (mobi.chouette.model.StopArea stopArea : stopAreas) {
                if (!exportableNetexData.getSharedStopPlaces().containsKey(stopArea.getObjectId())) {
                    StopPlace stopPlace = stopPlaceProducer.produce(context, stopArea);
                    exportableNetexData.getSharedStopPlaces().put(stopArea.getObjectId(), stopPlace);
                }
            }
        }
        List<Route> activeRoutes = exportableData.getVehicleJourneys().stream().map(vj -> vj.getRoute()).distinct().collect(Collectors.toList());
        produceAndCollectDestinationDisplays(activeRoutes, exportableNetexData);
    }

    @SuppressWarnings("unchecked")
    private void produceAndCollectCodespaces(Context context, ExportableNetexData exportableNetexData) {
        Set<mobi.chouette.model.Codespace> validCodespaces = (Set<mobi.chouette.model.Codespace>) context.get(NETEX_VALID_CODESPACES);

        for (mobi.chouette.model.Codespace validCodespace : validCodespaces) {
            if (!exportableNetexData.getSharedCodespaces().containsKey(validCodespace.getXmlns())) {
                org.rutebanken.netex.model.Codespace netexCodespace = netexFactory.createCodespace().withId(validCodespace.getXmlns().toLowerCase())
                        .withXmlns(validCodespace.getXmlns()).withXmlnsUrl(validCodespace.getXmlnsUrl());

                exportableNetexData.getSharedCodespaces().put(validCodespace.getXmlns(), netexCodespace);
            }
        }
    }

    private GroupOfLines createGroupOfLines(GroupOfLine groupOfLine) {
        GroupOfLines groupOfLines = netexFactory.createGroupOfLines();
        NetexProducerUtils.populateId(groupOfLine, groupOfLines);
        groupOfLines.setName(ConversionUtil.getMultiLingualString(groupOfLine.getName()));

        return groupOfLines;
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

                String scheduledStopPointId = chouetteScheduledStopPoint.getObjectId();

                if (!exportableNetexData.getSharedScheduledStopPoints().containsKey(scheduledStopPointId)) {
                    ScheduledStopPoint scheduledStopPoint = netexFactory.createScheduledStopPoint();
                    NetexProducerUtils.populateId(chouetteScheduledStopPoint, scheduledStopPoint);
//					scheduledStopPoint.setName(ConversionUtil.getMultiLingualString(chouetteScheduledStopPoint.getName()));
                    exportableNetexData.getSharedScheduledStopPoints().put(scheduledStopPointId + ":LOC", scheduledStopPoint);
                }
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

        if (!exportableNetexData.getSharedDestinationDisplays().containsKey(dd.getObjectId())) {

            DestinationDisplay netexDestinationDisplay = netexFactory.createDestinationDisplay();
            NetexProducerUtils.populateId(dd, netexDestinationDisplay);

//			netexDestinationDisplay.setName(ConversionUtil.getMultiLingualString(dd.getName()));
            netexDestinationDisplay.setFrontText(ConversionUtil.getMultiLingualString(dd.getFrontText()));
//			netexDestinationDisplay.setSideText(ConversionUtil.getMultiLingualString(dd.getSideText()));

            exportableNetexData.getSharedDestinationDisplays().put(dd.getObjectId(), netexDestinationDisplay);

//			if (dd.getVias() != null && dd.getVias().size() > 0) {
//				Vias_RelStructure vias = netexFactory.createVias_RelStructure();
//				netexDestinationDisplay.setVias(vias);
//				for (mobi.chouette.model.DestinationDisplay via : dd.getVias()) {
//
//					// Recurse into vias, create if missing
//					addDestinationDisplay(via, exportableNetexData);
//
//					DestinationDisplayRefStructure ref = netexFactory.createDestinationDisplayRefStructure();
//					NetexProducerUtils.populateReference(via, ref, true);
//
//					Via_VersionedChildStructure e = netexFactory.createVia_VersionedChildStructure().withDestinationDisplayRef(ref);
//
//					netexDestinationDisplay.getVias().getVia().add(e);
//				}
//			}

        }

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

            if (!exportableNetexData.getSharedStopAssignments().containsKey(passengerStopAssignmentId)) {
                PassengerStopAssignment stopAssignment = createPassengerStopAssignment(scheduledStopPoint, passengerStopAssignmentId, exportableNetexData.getSharedStopAssignments().size() + 1, parameters);
                exportableNetexData.getSharedStopAssignments().put(passengerStopAssignmentId, stopAssignment);
            }
        } else {
            throw new RuntimeException(
                    "ScheduledStopPoint with id : " + scheduledStopPoint.getObjectId() + " is not contained in a StopArea. Cannot produce StopAssignment.");
        }
    }

    private PassengerStopAssignment createPassengerStopAssignment(mobi.chouette.model.ScheduledStopPoint scheduledStopPoint, String stopAssignmentId, int order, NetexprofileExportParameters parameters) {
        PassengerStopAssignment passengerStopAssignment = netexFactory.createPassengerStopAssignment().withVersion(NETEX_DEFAULT_OBJECT_VERSION).withId(stopAssignmentId)
                .withOrder(BigInteger.valueOf(0));

        ScheduledStopPointRefStructure scheduledStopPointRef = netexFactory.createScheduledStopPointRefStructure();
        NetexProducerUtils.populateReference(scheduledStopPoint, scheduledStopPointRef, true);
        scheduledStopPointRef.setRef(scheduledStopPointRef.getRef() + ":LOC");
        scheduledStopPointRef.setVersion("any");
        passengerStopAssignment.setScheduledStopPointRef(netexFactory.createScheduledStopPointRef(scheduledStopPointRef));

        if (isSet(scheduledStopPoint.getContainedInStopAreaRef().getObject())) {
            mobi.chouette.model.StopArea containedInStopArea = scheduledStopPoint.getContainedInStopAreaRef().getObject();
            QuayRefStructure quayRefStruct = netexFactory.createQuayRefStructure();
            NetexProducerUtils.populateReference(containedInStopArea, quayRefStruct, parameters.isExportStops());

            passengerStopAssignment.setQuayRef(quayRefStruct);
        }

        passengerStopAssignment.setId(passengerStopAssignment.getId() + ":LOC");
        passengerStopAssignment.setVersion("any");

        return passengerStopAssignment;
    }

}
