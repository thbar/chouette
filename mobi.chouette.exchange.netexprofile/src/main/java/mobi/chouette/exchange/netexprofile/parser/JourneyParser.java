package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.model.*;
import mobi.chouette.model.Route;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.JourneyPattern;

import javax.xml.bind.JAXBElement;
import java.sql.Time;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j
public class JourneyParser implements Parser, Constant {

    private static final ZoneId LOCAL_ZONE_ID = ZoneId.of("Europe/Oslo");

    private Map<String, StopPointInJourneyPattern> stopPointInJourneyPatternMap = new HashMap<>();

    @Override
    public void parse(Context context) throws Exception {
        RelationshipStructure relationshipStruct = (RelationshipStructure) context.get(NETEX_LINE_DATA_CONTEXT);

        if (relationshipStruct instanceof JourneyPatternsInFrame_RelStructure) {
            JourneyPatternsInFrame_RelStructure contextData = (JourneyPatternsInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);
            List<JAXBElement<?>> journeyPatternElements = contextData.getJourneyPattern_OrJourneyPatternView();

            for (JAXBElement<?> journeyPatternElement : journeyPatternElements) {
                org.rutebanken.netex.model.JourneyPattern netexJourneyPattern = (org.rutebanken.netex.model.JourneyPattern) journeyPatternElement.getValue();
                parseJourneyPattern(context, netexJourneyPattern);
            }
        } else if (relationshipStruct instanceof JourneysInFrame_RelStructure) {
            JourneysInFrame_RelStructure journeysInFrameRelStruct = (JourneysInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);
            List<Journey_VersionStructure> serviceJourneyStructs = journeysInFrameRelStruct.getDatedServiceJourneyOrDeadRunOrServiceJourney();

            for (Journey_VersionStructure serviceJourneyStruct : serviceJourneyStructs) {

                // TODO handle all types of netex journeys, for now only parsing ServiceJourney instances
                ServiceJourney serviceJourney = (ServiceJourney) serviceJourneyStruct;
                parseServiceJourney(context, serviceJourney);
            }
        }
    }

    private void parseJourneyPattern(Context context, JourneyPattern netexJourneyPattern) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        mobi.chouette.model.JourneyPattern chouetteJourneyPattern = ObjectFactory.getJourneyPattern(referential, netexJourneyPattern.getId());

        Integer version = Integer.valueOf(netexJourneyPattern.getVersion());
        chouetteJourneyPattern.setObjectVersion(version != null ? version : 0);

        String routeIdRef = netexJourneyPattern.getRouteRef().getRef();
        mobi.chouette.model.Route route = ObjectFactory.getRoute(referential, routeIdRef);
        chouetteJourneyPattern.setRoute(route);

        if (netexJourneyPattern.getName() != null) {
            chouetteJourneyPattern.setName(netexJourneyPattern.getName().getValue());
        } else {
            chouetteJourneyPattern.setName(route.getName());
        }

        if (netexJourneyPattern.getPrivateCode() != null) {
            chouetteJourneyPattern.setRegistrationNumber(netexJourneyPattern.getPrivateCode().getValue());
        }

        PointsInJourneyPattern_RelStructure pointsInSequenceStruct = netexJourneyPattern.getPointsInSequence();
        List<PointInLinkSequence_VersionedChildStructure> pointsInLinkSequence = pointsInSequenceStruct
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        pointsInLinkSequence.forEach(point -> stopPointInJourneyPatternMap.put(point.getId(), (StopPointInJourneyPattern) point));

        route.getStopPoints().forEach(chouetteJourneyPattern::addStopPoint);
        chouetteJourneyPattern.getStopPoints().sort(Comparator.comparing(StopPoint::getPosition));
        chouetteJourneyPattern.setDepartureStopPoint(chouetteJourneyPattern.getStopPoints().get(0));
        chouetteJourneyPattern.setArrivalStopPoint(chouetteJourneyPattern.getStopPoints().get(chouetteJourneyPattern.getStopPoints().size() - 1));

        // TODO: add all remaining optional elements, for now we only support RouteRef and pointsInSequence.
        //      See: https://rutebanken.atlassian.net/wiki/display/PUBLIC/network#network-JourneyPattern

        chouetteJourneyPattern.setFilled(true);
    }

    private void parseServiceJourney(Context context, ServiceJourney serviceJourney) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential, serviceJourney.getId());

        Integer version = Integer.valueOf(serviceJourney.getVersion());
        vehicleJourney.setObjectVersion(version != null ? version : 0);

        // TODO check out if this gives the problem with journey names in digitransit (OSL-BGO instead of SK4887)
        if (serviceJourney.getName() != null) {
            vehicleJourney.setPublishedJourneyName(serviceJourney.getName().getValue());
        }
        vehicleJourney.setPublishedJourneyIdentifier(serviceJourney.getPublicCode());

        List<JAXBElement<? extends DayTypeRefStructure>> dayTypeRefStructElements = serviceJourney.getDayTypes().getDayTypeRef();
        for (JAXBElement<? extends DayTypeRefStructure> dayTypeRefStructElement : dayTypeRefStructElements) {
            String dayTypeIdRef = dayTypeRefStructElement.getValue().getRef();
            Timetable timetable = ObjectFactory.getTimetable(referential, dayTypeIdRef);
            timetable.addVehicleJourney(vehicleJourney);
        }

        String journeyPatternIdRef = serviceJourney.getJourneyPatternRef().getValue().getRef();
        mobi.chouette.model.JourneyPattern journeyPattern = ObjectFactory.getJourneyPattern(referential, journeyPatternIdRef);
        vehicleJourney.setJourneyPattern(journeyPattern);

        if (serviceJourney.getOperatorRef() != null) {
            String operatorIdRef = serviceJourney.getOperatorRef().getRef();
            Company company = ObjectFactory.getCompany(referential, operatorIdRef);
            vehicleJourney.setCompany(company);
        } else if (serviceJourney.getLineRef() != null) {
            String lineIdRef = serviceJourney.getLineRef().getValue().getRef();
            Company company = ObjectFactory.getLine(referential, lineIdRef).getCompany();
            vehicleJourney.setCompany(company);
        } else {
            Company company = journeyPattern.getRoute().getLine().getCompany();
            vehicleJourney.setCompany(company);
        }

        if (serviceJourney.getRouteRef() != null) {
            Route route = ObjectFactory.getRoute(referential, serviceJourney.getRouteRef().getRef());
            vehicleJourney.setRoute(route);
        } else {
            Route route = journeyPattern.getRoute();
            vehicleJourney.setRoute(route);
        }

        parseTimetabledPassingTimes(serviceJourney, vehicleJourney, journeyPattern);
        vehicleJourney.setFilled(true);
    }

    @SuppressWarnings("unchecked")
    private void parseTimetabledPassingTimes(ServiceJourney serviceJourney, VehicleJourney vehicleJourney, mobi.chouette.model.JourneyPattern journeyPattern) {
        List<TimetabledPassingTime> timetabledPassingTimes = serviceJourney.getPassingTimes().getTimetabledPassingTime();

        for (int i = 0; i < timetabledPassingTimes.size(); i++) {
            TimetabledPassingTime timetabledPassingTime = timetabledPassingTimes.get(i);

            VehicleJourneyAtStop vehicleJourneyAtStop = ObjectFactory.getVehicleJourneyAtStop();
            vehicleJourneyAtStop.setVehicleJourney(vehicleJourney);

            String pointInJourneyPatternId = timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef();
            StopPointInJourneyPattern stopPointInJourneyPattern = stopPointInJourneyPatternMap.get(pointInJourneyPatternId);

            StopPoint stopPoint = journeyPattern.getStopPoints().get(i);
            vehicleJourneyAtStop.setStopPoint(stopPoint);

            // Default = board and alight
            vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardAndAlight);

            Boolean forBoarding = stopPointInJourneyPattern.isForBoarding();
            Boolean forAlighting = stopPointInJourneyPattern.isForAlighting();

            if (forBoarding == null && forAlighting != null && !forAlighting) {
                vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardOnly);
            }
            if (forAlighting == null && forBoarding != null && !forBoarding) {
                vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.AlightOnly);
            }

            parsePassingTimes(timetabledPassingTime, vehicleJourneyAtStop);
        }

        vehicleJourney.getVehicleJourneyAtStops().sort(Comparator.comparingInt(o -> o.getStopPoint().getPosition()));
    }

    // TODO add support for other time zones and zone offsets, for now only handling UTC
    private void parsePassingTimes(TimetabledPassingTime timetabledPassingTime, VehicleJourneyAtStop vehicleJourneyAtStop) {
        OffsetTime departureTime = timetabledPassingTime.getDepartureTime();
        OffsetTime arrivalTime = timetabledPassingTime.getArrivalTime();

        if (departureTime != null) {
            ZoneOffset zoneOffset = departureTime.getOffset();

            if (zoneOffset.equals(ZoneOffset.UTC)) {
                Time localDepartureTime = NetexUtils.convertToSqlTime(departureTime, NetexUtils.getZoneOffset(LOCAL_ZONE_ID));
                vehicleJourneyAtStop.setDepartureTime(localDepartureTime);
            }

            // TODO: add support for zone offsets other than utc here  (like +02:00, -05:30, etc...)
        }
        if (arrivalTime != null) {
            ZoneOffset zoneOffset = arrivalTime.getOffset();

            if (zoneOffset.equals(ZoneOffset.UTC)) {
                Time localArrivalTime = NetexUtils.convertToSqlTime(arrivalTime, NetexUtils.getZoneOffset(LOCAL_ZONE_ID));
                vehicleJourneyAtStop.setArrivalTime(localArrivalTime);
            }

            // TODO: add support for zone offsets other than utc here (like +02:00, -05:30, etc...)

        } else {
            // TODO find out if necessary
            // vehicleJourneyAtStop.setArrivalTime(new Time(vehicleJourneyAtStop.getDepartureTime().getTime()));
        }

    }

    static {
        ParserFactory.register(JourneyParser.class.getName(), new ParserFactory() {
            private JourneyParser instance = new JourneyParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }

}
