package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.model.ObjectReference;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import org.rutebanken.netex.model.MultilingualString;

public class DirectionProducer extends NetexProducer {

    public org.rutebanken.netex.model.Direction produce(StopPoint stopPoint) {
        org.rutebanken.netex.model.Direction netexDirection = netexFactory.createDirection();
        netexDirection.setId(stopPoint.getRoute().getObjectId().replace("Route", "Direction") + ":LOC");
        netexDirection.setVersion("any");
        ObjectReference<StopArea> stopArea = stopPoint.getScheduledStopPoint().getContainedInStopAreaRef();
        MultilingualString directionName = new MultilingualString();
        directionName.setValue(stopArea.getObject().getName());
        netexDirection.setName(directionName);

        return netexDirection;
    }

}
