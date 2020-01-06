package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.StopPoint;
import org.rutebanken.netex.model.DestinationDisplayRefStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.RouteRefStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

public class ServiceJourneyPatternProducer extends NetexProducer {

    public org.rutebanken.netex.model.ServiceJourneyPattern produce(JourneyPattern journeyPattern) {
        org.rutebanken.netex.model.ServiceJourneyPattern netexServiceJourneyPattern = netexFactory.createServiceJourneyPattern();
        netexServiceJourneyPattern.setId(journeyPattern.getObjectId().replace("JourneyPattern", "ServiceJourneyPattern") + ":LOC");
        netexServiceJourneyPattern.setVersion("any");

        MultilingualString serviceJourneyPatternName = new MultilingualString();
        serviceJourneyPatternName.setValue(journeyPattern.getName());
        netexServiceJourneyPattern.setName(serviceJourneyPatternName);

        RouteRefStructure routeRefStructure = new RouteRefStructure();
        routeRefStructure.setRef(journeyPattern.getRoute().getObjectId() + ":LOC");
        routeRefStructure.setVersion("any");
        netexServiceJourneyPattern.setRouteRef(routeRefStructure);

        for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
            if (stopPoint != null) {
                mobi.chouette.model.DestinationDisplay dd = stopPoint.getDestinationDisplay();
                if (dd != null && dd.getFrontText() != null && !dd.getFrontText().isEmpty()) {
                    DestinationDisplayRefStructure destinationDisplayRefStructure = new DestinationDisplayRefStructure();
                    destinationDisplayRefStructure.setRef(dd.getObjectId() + ":LOC");
                    destinationDisplayRefStructure.setVersion("any");
                    netexServiceJourneyPattern.setDestinationDisplayRef(destinationDisplayRefStructure);
                }
            }
        }


        PointsInJourneyPattern_RelStructure pointsInJourneyPattern_relStructure = new PointsInJourneyPattern_RelStructure();
        Collection<PointInLinkSequence_VersionedChildStructure> pointInLinkSequence_versionedChildStructures = new ArrayList<>();

        for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
            StopPointInJourneyPattern stopPointInJourneyPattern = new StopPointInJourneyPattern();
            NetexProducerUtils.populateId(stopPoint, stopPointInJourneyPattern);
            stopPointInJourneyPattern.setVersion("any");

            stopPointInJourneyPattern.setOrder(BigInteger.valueOf(stopPoint.getPosition() + 1));

            ScheduledStopPointRefStructure scheduledStopPointRefStructure = netexFactory.createScheduledStopPointRefStructure();
            scheduledStopPointRefStructure.setVersion("any");
            NetexProducerUtils.populateReference(stopPoint.getScheduledStopPoint(), scheduledStopPointRefStructure, false);
            scheduledStopPointRefStructure.setRef(scheduledStopPointRefStructure.getRef() + ":LOC");
            stopPointInJourneyPattern.setScheduledStopPointRef(netexFactory.createScheduledStopPointRef(scheduledStopPointRefStructure));

            pointInLinkSequence_versionedChildStructures.add(stopPointInJourneyPattern);

        }

        pointsInJourneyPattern_relStructure.withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(pointInLinkSequence_versionedChildStructures);
        netexServiceJourneyPattern.setPointsInSequence(pointsInJourneyPattern_relStructure);

        return netexServiceJourneyPattern;
    }
}
