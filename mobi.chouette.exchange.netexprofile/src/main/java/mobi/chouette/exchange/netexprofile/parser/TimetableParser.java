package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.ParserUtils;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.util.NetexObjectUtil;
import mobi.chouette.exchange.netexprofile.importer.util.NetexReferential;
import mobi.chouette.model.*;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang.StringUtils;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.sql.Date;
import java.sql.Time;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Log4j
public class TimetableParser implements NetexParser {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId LOCAL_ZONE_ID = ZoneId.of("Europe/Oslo");

    @Override
    public void initReferentials(Context context) throws Exception {
        NetexReferential referential = (NetexReferential) context.get(NETEX_REFERENTIAL);
    }

    @Override
    public void parse(Context context) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);

        TimetableFrame timetableFrame = (TimetableFrame) context.get(NETEX_LINE_DATA_CONTEXT);

        ValidityConditions_RelStructure validityConditions = timetableFrame.getValidityConditions();

        if (validityConditions != null) {
            List<Object> availabilityConditionElements = validityConditions.getValidityConditionRefOrValidBetweenOrValidityCondition_();
            // should iterate all availability conditions, for now only retrieving first occurrence
/*
            for (JAXBElement<AvailabilityCondition> availabilityConditionElement : availabilityConditionElements) {
                AvailabilityCondition value = availabilityConditionElement.getValue();
            }
*/
            // TODO: add more sophisticated check on zoneids and zoneoffsets here
            // how to connect the period to the right timetable instance? we can only get timetables by day type id
            if (availabilityConditionElements != null && availabilityConditionElements.size() > 0) {
                AvailabilityCondition availabilityCondition = ((JAXBElement<AvailabilityCondition>) availabilityConditionElements.get(0)).getValue();
                OffsetDateTime fromDate = availabilityCondition.getFromDate();
                OffsetDateTime toDate = availabilityCondition.getToDate();
                Date startOfPeriod = ParserUtils.getSQLDate(fromDate.toString());
                Date endOfPeriod = ParserUtils.getSQLDate(toDate.toString());
                Period period = new Period(startOfPeriod, endOfPeriod);
                //timetable.addPeriod(period);
            }
        }

        JourneysInFrame_RelStructure vehicleJourneysStruct = timetableFrame.getVehicleJourneys();

        if (vehicleJourneysStruct != null) {
            List<Journey_VersionStructure> serviceJourneyStructs = vehicleJourneysStruct.getDatedServiceJourneyOrDeadRunOrServiceJourney();

            for (Journey_VersionStructure serviceJourneyStruct : serviceJourneyStructs) {
                ServiceJourney serviceJourney = (ServiceJourney) serviceJourneyStruct;
                VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential, serviceJourney.getId());
                JourneyPattern journeyPattern = null;

                // TODO implement the following?
                //vehicleJourney.setPublishedJourneyName(departureText.getDestinationText());
                //vehicleJourney.setPublishedJourneyIdentifier(StringUtils.trimToNull(trip.getLineNumberVisible()));
                //vehicleJourney.setTransportMode(convertTypeOfService(trip.getTypeOfService()));

                OffsetTime serviceJourneyDepartureTime = serviceJourney.getDepartureTime();
                DayTypeRefs_RelStructure dayTypesStruct = serviceJourney.getDayTypes();
                if (dayTypesStruct != null) {
                    List<JAXBElement<? extends DayTypeRefStructure>> dayTypeRefElements = dayTypesStruct.getDayTypeRef();

                    for (JAXBElement<? extends DayTypeRefStructure> dayTypeRefElement : dayTypeRefElements) {
                        DayTypeRefStructure dayTypeRefStructure = dayTypeRefElement.getValue();
                        String dayTypeRef = dayTypeRefStructure.getRef();
                        Timetable timetable = referential.getTimetables().get(dayTypeRef);

                        if (timetable != null) {
                            // TODO find out if this should be the opposite
                            vehicleJourney.getTimetables().add(timetable);
                            //timetable.addVehicleJourney(vehicleJourney);
                        }
                    }
                }

                JAXBElement<? extends JourneyPatternRefStructure> journeyPatternRefStructElement = serviceJourney.getJourneyPatternRef();
                if (journeyPatternRefStructElement != null) {
                    JourneyPatternRefStructure journeyPatternRefStructure = journeyPatternRefStructElement.getValue();
                    String journeyPatternId = journeyPatternRefStructure.getRef();
                    journeyPattern = ObjectFactory.getJourneyPattern(referential, journeyPatternId);
                    vehicleJourney.setJourneyPattern(journeyPattern);
                }

                // String publicCode = serviceJourney.getPublicCode(); // how to handle in chouette model?

                OperatorRefStructure operatorRefStruct = serviceJourney.getOperatorRef();
                if (operatorRefStruct != null) {
                    String operatorId = operatorRefStruct.getRef();
                    Company company = ObjectFactory.getCompany(referential, operatorId);
                    vehicleJourney.setCompany(company);
                }

                // We actually must have a RouteRef in each ServiceJourney instance, if not we get a np in VehicleJourneyUpdater#update:208
                // TODO: probably need schema change too, as RouteRef is not a valid element of a ServiceJourney in the NO-profile
                RouteRefStructure routeRefStruct = serviceJourney.getRouteRef();
                if (routeRefStruct != null) {
                    String routeId = routeRefStruct.getRef();
                    Route route = ObjectFactory.getRoute(referential, routeId);
                    vehicleJourney.setRoute(route);
                } else {
                    // TODO: remove temp else block, when RouteRef supported in NO-profile of netex
                    JAXBElement<? extends LineRefStructure> lineRefStructElement = serviceJourney.getLineRef();
                    if (lineRefStructElement != null) {
                        LineRefStructure lineRefStructure = lineRefStructElement.getValue();
                        String lineId = lineRefStructure.getRef();
                        Line line = ObjectFactory.getLine(referential, lineId);
                        Route route = line.getRoutes().get(0);
                        vehicleJourney.setRoute(route);
                    }
                }

                // TODO: must have a RouteRef in ServiceJourney instead of LineRef, chouette model only supports references to Routes
                JAXBElement<? extends LineRefStructure> lineRefStructElement = serviceJourney.getLineRef();
                if (lineRefStructElement != null) {
                    LineRefStructure lineRefStructure = lineRefStructElement.getValue();
                    String lineId = lineRefStructure.getRef();
                    Line line = ObjectFactory.getLine(referential, lineId);
                    //vehicleJourney.setLIne(line);
                }

                TimetabledPassingTimes_RelStructure timetabledPassingTimesStruct = serviceJourney.getPassingTimes();
                if (timetabledPassingTimesStruct != null) {
                    List<TimetabledPassingTime> timetabledPassingTimes = timetabledPassingTimesStruct.getTimetabledPassingTime();
                    if (timetabledPassingTimes != null && timetabledPassingTimes.size() > 0) {

                        int index = 1;
                        for (TimetabledPassingTime timetabledPassingTime : timetabledPassingTimes) {
                            NetexReferential netexReferential = (NetexReferential) context.get(NETEX_REFERENTIAL);

                            // TODO figure out if this is correct (done in regtopp conversion)
                            VehicleJourneyAtStop vehicleJourneyAtStop = ObjectFactory.getVehicleJourneyAtStop();
                            vehicleJourneyAtStop.setVehicleJourney(vehicleJourney);

                            // TODO find out where this should go, probably inside loop for timetable passing times
                            // Default = board and alight
/*
                            vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardAndAlight);

                            if (driverTimeArrival == null && driverTimeDeparture == null) {
                                // Both 999
                                vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.NeitherBoardOrAlight);
                                // What to do with passing times?
                            } else {
                                if (driverTimeArrival != null) {
                                    setArrival(tripDepartureTime, driverTimeArrival, vehicleJourneyAtStop);
                                } else {
                                    setArrival(tripDepartureTime, driverTimeDeparture, vehicleJourneyAtStop);
                                    vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardOnly);
                                }

                                if (driverTimeDeparture != null) {
                                    setDeparture(tripDepartureTime, driverTimeDeparture, vehicleJourneyAtStop);
                                } else {
                                    setDeparture(tripDepartureTime, driverTimeArrival, vehicleJourneyAtStop);
                                    vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.AlightOnly);
                                }
                            }
*/

                            JAXBElement<? extends PointInJourneyPatternRefStructure> pointInJourneyPatternRefStructElement =
                                    timetabledPassingTime.getPointInJourneyPatternRef();

                            if (pointInJourneyPatternRefStructElement != null) {
                                PointInJourneyPatternRefStructure pointInJourneyPatternRefStruct = pointInJourneyPatternRefStructElement.getValue();
                                String pointInJourneyPatternId = pointInJourneyPatternRefStruct.getRef();
                                StopPointInJourneyPattern stopPointInJourneyPattern = NetexObjectUtil.getStopPointInJourneyPattern(netexReferential, pointInJourneyPatternId);

                                if (stopPointInJourneyPattern != null) {
                                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRefElement = stopPointInJourneyPattern.getScheduledStopPointRef();

                                    if (scheduledStopPointRefElement != null) {
                                        ScheduledStopPointRefStructure scheduledStopPointRefStruct = scheduledStopPointRefElement.getValue();

                                        if (scheduledStopPointRefStruct != null) {
                                            String scheduledStopPointRefValue= scheduledStopPointRefStruct.getRef();

                                            if (StringUtils.isNotEmpty(scheduledStopPointRefValue)) {

                                                // TODO same problem as in JourneyPatternParser, line 91, with ids, temporary generate to get the correcxt stop point
                                                // temp fix for id problem

                                                // TODO fix the ids
                                                String chouetteStopPointId =  scheduledStopPointRefValue + "-" + index;
                                                //stopPoint = ObjectFactory.getStopPoint(chouetteReferential, chouetteStopPointId);
                                                //chouetteJourneyPattern.addStopPoint(stopPoint);

                                                List<StopPoint> stopPoints = journeyPattern.getStopPoints();

                                                for (StopPoint stopPoint : stopPoints) {
                                                    //if (stopPoint.getObjectId().equals(scheduledStopPointRefValue)) {
                                                    if (stopPoint.getObjectId().equals(chouetteStopPointId)) {
                                                        vehicleJourneyAtStop.setStopPoint(stopPoint);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // TODO figure out if this is correct or wrong (not done in regtopp conversion)
                            //vehicleJourney.getVehicleJourneyAtStops().add(vehicleJourneyAtStop);

                            // following is a temporary solution for handling incoming data in UTC
                            OffsetTime arrivalTime = timetabledPassingTime.getArrivalTime();
                            if (arrivalTime != null) {
                                ZoneOffset zoneOffset = arrivalTime.getOffset();
                                if (zoneOffset.equals(ZoneOffset.UTC)) {
                                    Time localArrivalTime = NetexUtils.convertToSqlTime(arrivalTime, NetexUtils.getZoneOffset(LOCAL_ZONE_ID));
                                    vehicleJourneyAtStop.setArrivalTime(localArrivalTime);
                                }
                                // TODO: add support for zone offsets other than utc here (like +02:00, -05:30, etc...)
                            }

                            OffsetTime departureTime = timetabledPassingTime.getDepartureTime();
                            if (departureTime != null) {
                                ZoneOffset zoneOffset = departureTime.getOffset();
                                if (zoneOffset.equals(ZoneOffset.UTC)) {
                                    Time localDepartureTime = NetexUtils.convertToSqlTime(departureTime, NetexUtils.getZoneOffset(LOCAL_ZONE_ID));
                                    vehicleJourneyAtStop.setDepartureTime(localDepartureTime);
                                }
                                // TODO: add support for zone offsets other than utc here  (like +02:00, -05:30, etc...)
                            }

                            index++;
                        }
                    }
                }
                vehicleJourney.setFilled(true);
            }
        }
    }

    static {
        ParserFactory.register(TimetableParser.class.getName(), new ParserFactory() {
            private TimetableParser instance = new TimetableParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }

}
