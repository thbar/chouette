package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.StopPoint;
import org.rutebanken.netex.model.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

public class ServiceJourneyPatternIDFMProducer extends NetexProducer {

    public org.rutebanken.netex.model.ServiceJourneyPattern produce(JourneyPattern journeyPattern, ExportableNetexData exportableNetexData) {
        org.rutebanken.netex.model.ServiceJourneyPattern netexServiceJourneyPattern = netexFactory.createServiceJourneyPattern();

        NetexProducerUtils.populateIdAndVersionIDFM(journeyPattern, netexServiceJourneyPattern);

        MultilingualString serviceJourneyPatternName = new MultilingualString();
        serviceJourneyPatternName.setValue(journeyPattern.getName());
        netexServiceJourneyPattern.setName(serviceJourneyPatternName);

        RouteRefStructure routeRefStructure = new RouteRefStructure();
        NetexProducerUtils.populateReferenceIDFM(journeyPattern.getRoute(), routeRefStructure);
        netexServiceJourneyPattern.setRouteRef(routeRefStructure);

        for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
            if (stopPoint != null) {
                mobi.chouette.model.DestinationDisplay dd = stopPoint.getDestinationDisplay();
                if (dd != null && dd.getFrontText() != null && !dd.getFrontText().isEmpty()) {
                    DestinationDisplayRefStructure destinationDisplayRefStructure = new DestinationDisplayRefStructure();
                    destinationDisplayRefStructure.setRef(dd.getObjectId() + ":LOC");
                    destinationDisplayRefStructure.setVersion("any");
                    netexServiceJourneyPattern.setDestinationDisplayRef(destinationDisplayRefStructure);

                    DestinationDisplay netexDestinationDisplay = netexFactory.createDestinationDisplay();
                    netexDestinationDisplay.setId(destinationDisplayRefStructure.getRef());
                    netexDestinationDisplay.setVersion("any");
                    netexDestinationDisplay.setFrontText(ConversionUtil.getMultiLingualString(dd.getFrontText()));
                    exportableNetexData.getSharedDestinationDisplays().put(netexDestinationDisplay.getId(), netexDestinationDisplay);

                    }
            }
        }


        PointsInJourneyPattern_RelStructure pointsInJourneyPattern_relStructure = new PointsInJourneyPattern_RelStructure();
        Collection<PointInLinkSequence_VersionedChildStructure> pointInLinkSequence_versionedChildStructures = new ArrayList<>();

        for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
            StopPointInJourneyPattern stopPointInJourneyPattern = new StopPointInJourneyPattern();
            NetexProducerUtils.populateIdAndVersionIDFM(stopPoint, stopPointInJourneyPattern);

            stopPointInJourneyPattern.setOrder(BigInteger.valueOf(stopPoint.getPosition() + 1));

            ScheduledStopPointRefStructure scheduledStopPointRefStructure = netexFactory.createScheduledStopPointRefStructure();
            NetexProducerUtils.populateReferenceIDFM(stopPoint.getScheduledStopPoint(), scheduledStopPointRefStructure);
            stopPointInJourneyPattern.setScheduledStopPointRef(netexFactory.createScheduledStopPointRef(scheduledStopPointRefStructure));

            ScheduledStopPoint scheduledStopPoint = netexFactory.createScheduledStopPoint();
            scheduledStopPoint.setId(scheduledStopPointRefStructure.getRef());
            scheduledStopPoint.setVersion("any");
            exportableNetexData.getSharedScheduledStopPoints().put(scheduledStopPoint.getId(), scheduledStopPoint);

            pointInLinkSequence_versionedChildStructures.add(stopPointInJourneyPattern);

        }

        pointsInJourneyPattern_relStructure.withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(pointInLinkSequence_versionedChildStructures);
        netexServiceJourneyPattern.setPointsInSequence(pointsInJourneyPattern_relStructure);

        netexServiceJourneyPattern.setServiceJourneyPatternType(ServiceJourneyPatternTypeEnumeration.PASSENGER);

        return netexServiceJourneyPattern;
    }
}
