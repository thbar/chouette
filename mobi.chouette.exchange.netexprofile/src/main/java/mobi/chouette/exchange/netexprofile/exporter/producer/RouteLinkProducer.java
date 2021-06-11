package mobi.chouette.exchange.netexprofile.exporter.producer;

import com.vividsolutions.jts.geom.LineString;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.util.JtsGmlConverter;
import net.opengis.gml._3.LineStringType;
import org.rutebanken.netex.model.RoutePointRefStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;


@Log4j
public class RouteLinkProducer extends NetexProducer implements NetexEntityProducer<org.rutebanken.netex.model.RouteLink, mobi.chouette.model.RouteSection> {

    @Override
    public org.rutebanken.netex.model.RouteLink produce(Context context, mobi.chouette.model.RouteSection neptuneRouteSection) {
        org.rutebanken.netex.model.RouteLink netexLink = netexFactory.createRouteLink();
        cleanObjectId(neptuneRouteSection);
        NetexProducerUtils.populateIdAndVersionIDFM(neptuneRouteSection, netexLink);
        netexLink.setDistance(neptuneRouteSection.getDistance());
        netexLink.setLineString(getLineStringFromRouteSection(neptuneRouteSection));


        ScheduledStopPointRefStructure scheduledStopPointFromRef = netexFactory.createScheduledStopPointRefStructure();
        NetexProducerUtils.populateReferenceIDFM(neptuneRouteSection.getFromScheduledStopPoint(), scheduledStopPointFromRef);
        netexLink.setFromPointRef(scheduledStopPointFromRef);

        ScheduledStopPointRefStructure scheduledStopPointToRef = netexFactory.createScheduledStopPointRefStructure();
        NetexProducerUtils.populateReferenceIDFM(neptuneRouteSection.getToScheduledStopPoint(), scheduledStopPointToRef);
        netexLink.setToPointRef(scheduledStopPointToRef);

        netexLink.setVersion(NETEX_DEFAULT_OBJECT_VERSION);

        return netexLink;
    }

    private LineStringType getLineStringFromRouteSection(mobi.chouette.model.RouteSection neptuneRouteSection){
        if (neptuneRouteSection.getProcessedGeometry() != null)
            return convertLineString(neptuneRouteSection.getProcessedGeometry(),neptuneRouteSection.getId().toString());

        return convertLineString(neptuneRouteSection.getInputGeometry(),neptuneRouteSection.getId().toString());
    }

    private LineStringType convertLineString(LineString lineString, String routeSectionId){
        return JtsGmlConverter.fromJtsToGml(lineString,"LS"+routeSectionId);
    }

    private void cleanObjectId(mobi.chouette.model.RouteSection neptuneRouteSection){
        String objectId = neptuneRouteSection.getObjectId();
        if (objectId.split(":").length > 3){
            String[] idComponents = objectId.split(":RouteSection:");
            String newId = idComponents[0] + ":RouteSection:" + idComponents[1].replaceAll(":","_");
            neptuneRouteSection.setObjectId(newId);
        }
    }

}
