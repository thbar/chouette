package mobi.chouette.exchange.netexprofile.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.model.type.PTDirectionEnum;
import org.rutebanken.netex.model.DirectionRefStructure;
import org.rutebanken.netex.model.DirectionTypeEnumeration;
import org.rutebanken.netex.model.RouteRefStructure;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;

@Log4j
public class RouteIDFMProducer extends NetexProducer implements NetexEntityProducer<org.rutebanken.netex.model.Route, mobi.chouette.model.Route> {

    @Override
    public org.rutebanken.netex.model.Route produce(Context context, mobi.chouette.model.Route neptuneRoute) {
        org.rutebanken.netex.model.Route netexRoute = netexFactory.createRoute();
        NetexProducerUtils.populateIdAndVersionIDFM(neptuneRoute, netexRoute);

        netexRoute.setName(ConversionUtil.getMultiLingualString(neptuneRoute.getName()));

        netexRoute.setLineRef(NetexProducerUtils.createLineIDFMRef(neptuneRoute.getLine(), netexFactory));

        netexRoute.setDirectionType(mapDirectionType(neptuneRoute.getDirection()));

        DirectionRefStructure directionRefStructure = new DirectionRefStructure();
        directionRefStructure.setRef(netexRoute.getId().replace("Route", "Direction"));
        directionRefStructure.setVersion("any");
        netexRoute.setDirectionRef(directionRefStructure);

        ExportableData exportableData = (ExportableData) context.get(Constant.EXPORTABLE_DATA);
        if (isSet(neptuneRoute.getOppositeRoute()) && exportableData.getRoutes().contains(neptuneRoute.getOppositeRoute())) {
            RouteRefStructure routeRefStruct = netexFactory.createRouteRefStructure();
            NetexProducerUtils.populateReferenceIDFM(neptuneRoute.getOppositeRoute(), routeRefStruct);
            netexRoute.setInverseRouteRef(routeRefStruct);
        }

        return netexRoute;
    }


    private DirectionTypeEnumeration mapDirectionType(PTDirectionEnum neptuneDirection) {
        if (neptuneDirection == null) {
            return null;
        }
        switch (neptuneDirection) {
            case A:
                return DirectionTypeEnumeration.OUTBOUND;
            case R:
                return DirectionTypeEnumeration.INBOUND;
            case ClockWise:
                return DirectionTypeEnumeration.CLOCKWISE;
            case CounterClockWise:
                return DirectionTypeEnumeration.ANTICLOCKWISE;
        }

        log.debug("Unable to map neptune direction to NeTEx: " + neptuneDirection);
        return null;
    }
}
