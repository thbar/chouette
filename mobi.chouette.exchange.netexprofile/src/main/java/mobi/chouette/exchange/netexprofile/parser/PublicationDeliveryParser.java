package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.importer.util.NetexObjectUtil;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.*;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.type.AlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingPossibilityEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.Network;

import javax.xml.bind.JAXBElement;
import java.util.*;

@Log4j
public class PublicationDeliveryParser extends AbstractParser {

	private Map<String, String> stopAssignments;

	@Override
	public void parse(Context context) throws Exception {
		boolean isCommonDelivery = context.get(NETEX_WITH_COMMON_DATA) != null && context.get(NETEX_LINE_DATA_JAVA) == null;
		Referential referential = (Referential) context.get(REFERENTIAL);
        String contextKey = isCommonDelivery ? NETEX_COMMON_DATA : NETEX_LINE_DATA_JAVA;
		PublicationDeliveryStructure publicationDelivery = (PublicationDeliveryStructure) context.get(contextKey);
		List<JAXBElement<? extends Common_VersionFrameStructure>> dataObjectFrames = publicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();

		List<ResourceFrame> resourceFrames = NetexObjectUtil.getFrames(ResourceFrame.class, dataObjectFrames);
		List<ServiceFrame> serviceFrames = NetexObjectUtil.getFrames(ServiceFrame.class, dataObjectFrames);
		List<SiteFrame> siteFrames = NetexObjectUtil.getFrames(SiteFrame.class, dataObjectFrames);
		List<ServiceCalendarFrame> serviceCalendarFrames = NetexObjectUtil.getFrames(ServiceCalendarFrame.class, dataObjectFrames);
		List<TimetableFrame> timetableFrames = new ArrayList<>();

		if (!isCommonDelivery) {
			timetableFrames = NetexObjectUtil.getFrames(TimetableFrame.class, dataObjectFrames);
		}

		// pre processing
		preParseReferentialDependencies(context, serviceFrames, isCommonDelivery);

		// normal processing
		parseResourceFrames(context, resourceFrames);
		parseSiteFrames(context, siteFrames);
		parseServiceFrames(context, serviceFrames, isCommonDelivery);
		parseServiceCalendarFrame(context, serviceCalendarFrames);

		if (!isCommonDelivery) {
			parseTimetableFrames(context, timetableFrames);
		}

		// post processing
		linkStopPointsToAssignedStopArea(referential);
		//sortStopPointsOnRoutes(referential);
		updateBoardingAlighting(referential);
	}

	private void preParseReferentialDependencies(Context context, List<ServiceFrame> serviceFrames, boolean isCommonDelivery) throws Exception {
		if (stopAssignments == null) {
			stopAssignments = new HashMap<>();
		}

		for (ServiceFrame serviceFrame : serviceFrames) {
			StopAssignmentsInFrame_RelStructure stopAssignmentsStructure = serviceFrame.getStopAssignments();
			if (stopAssignmentsStructure != null) {
				List<JAXBElement<? extends StopAssignment_VersionStructure>> stopAssignmentElements = stopAssignmentsStructure.getStopAssignment();

				for (JAXBElement<? extends StopAssignment_VersionStructure> stopAssignmentElement : stopAssignmentElements) {
					PassengerStopAssignment passengerStopAssignment = (PassengerStopAssignment) stopAssignmentElement.getValue();
					ScheduledStopPointRefStructure scheduledStopPointRef = passengerStopAssignment.getScheduledStopPointRef();
					StopPlaceRefStructure stopPlaceRef = passengerStopAssignment.getStopPlaceRef();

					if (scheduledStopPointRef != null && stopPlaceRef != null) {
					    if (!stopAssignments.containsKey(scheduledStopPointRef.getRef())) {
                            stopAssignments.put(scheduledStopPointRef.getRef(), stopPlaceRef.getRef());
                        }
					}
				}
			}

			if (!isCommonDelivery) {

				// preparsing mandatory for stop places to parse correctly
				TariffZonesInFrame_RelStructure tariffZonesStruct = serviceFrame.getTariffZones();
				if (tariffZonesStruct != null) {
					context.put(NETEX_LINE_DATA_CONTEXT, tariffZonesStruct);
					StopPlaceParser stopPlaceParser = (StopPlaceParser) ParserFactory.create(StopPlaceParser.class.getName());
					stopPlaceParser.parse(context);
				}
			} else {

			}
        }
    }

    private void parseResourceFrames(Context context, List<ResourceFrame> resourceFrames) throws Exception {
		for (ResourceFrame resourceFrame : resourceFrames) {
			OrganisationsInFrame_RelStructure organisationsInFrameStruct = resourceFrame.getOrganisations();
			if (organisationsInFrameStruct != null) {
				context.put(NETEX_LINE_DATA_CONTEXT, organisationsInFrameStruct);
				OrganisationParser organisationParser = (OrganisationParser) ParserFactory.create(OrganisationParser.class.getName());
				organisationParser.parse(context);
			}
		}
	}

	private void parseSiteFrames(Context context, List<SiteFrame> siteFrames) throws Exception {
		for (SiteFrame siteFrame : siteFrames) {
            StopPlacesInFrame_RelStructure stopPlacesStruct = siteFrame.getStopPlaces();
			if (stopPlacesStruct != null) {
				context.put(NETEX_LINE_DATA_CONTEXT, stopPlacesStruct);
				StopPlaceParser stopPlaceParser = (StopPlaceParser) ParserFactory.create(StopPlaceParser.class.getName());
				stopPlaceParser.parse(context);
			}
		}
	}

	private void parseServiceFrames(Context context, List<ServiceFrame> serviceFrames, boolean isCommonDelivery) throws Exception {
		for (ServiceFrame serviceFrame : serviceFrames) {
			if (!isCommonDelivery) {
				Network network = serviceFrame.getNetwork();
				context.put(NETEX_LINE_DATA_CONTEXT, network);
				NetworkParser networkParser = (NetworkParser) ParserFactory.create(NetworkParser.class.getName());
				networkParser.parse(context);

				LinesInFrame_RelStructure linesInFrameStruct = serviceFrame.getLines();
				context.put(NETEX_LINE_DATA_CONTEXT, linesInFrameStruct);
				LineParser lineParser = (LineParser) ParserFactory.create(LineParser.class.getName());
				lineParser.parse(context);

				RoutesInFrame_RelStructure routesInFrameStruct = serviceFrame.getRoutes();
				context.put(NETEX_LINE_DATA_CONTEXT, routesInFrameStruct);
				RouteParser routeParser = (RouteParser) ParserFactory.create(RouteParser.class.getName());
				routeParser.parse(context);
			}

			if (!isCommonDelivery) {
				JourneyPatternsInFrame_RelStructure journeyPatternStruct = serviceFrame.getJourneyPatterns();
				context.put(NETEX_LINE_DATA_CONTEXT, journeyPatternStruct);
                JourneyParser journeyParser = (JourneyParser) ParserFactory.create(JourneyParser.class.getName());
                journeyParser.parse(context);

				TransfersInFrame_RelStructure connectionsStruct = serviceFrame.getConnections();
				if (connectionsStruct != null) {
					// TODO implement connection link parser
				}
			}
		}
	}

	private void parseServiceCalendarFrame(Context context, List<ServiceCalendarFrame> serviceCalendarFrames) throws Exception {
		for (ServiceCalendarFrame serviceCalendarFrame : serviceCalendarFrames) {
            Parser calendarParser = ParserFactory.create(CalendarParser.class.getName());

            ValidityConditions_RelStructure validityConditionsStruct = serviceCalendarFrame.getContentValidityConditions();
            if (validityConditionsStruct != null) {
                context.put(NETEX_LINE_DATA_CONTEXT, validityConditionsStruct);
                calendarParser.parse(context);
            }
            DayTypesInFrame_RelStructure dayTypeStruct = serviceCalendarFrame.getDayTypes();
            if (dayTypeStruct != null) {
                context.put(NETEX_LINE_DATA_CONTEXT, dayTypeStruct);
                calendarParser.parse(context);
            }
            OperatingDaysInFrame_RelStructure operatingDaysStruct = serviceCalendarFrame.getOperatingDays();
            if (operatingDaysStruct != null) {
                context.put(NETEX_LINE_DATA_CONTEXT, operatingDaysStruct);
                calendarParser.parse(context);
            }
            OperatingPeriodsInFrame_RelStructure operatingPeriodsStruct = serviceCalendarFrame.getOperatingPeriods();
            if (operatingPeriodsStruct != null) {
                context.put(NETEX_LINE_DATA_CONTEXT, operatingPeriodsStruct);
                calendarParser.parse(context);
            }
		}
	}

	private void parseTimetableFrames(Context context, List<TimetableFrame> timetableFrames) throws Exception {
		for (TimetableFrame timetableFrame : timetableFrames) {
			JourneysInFrame_RelStructure vehicleJourneysStruct = timetableFrame.getVehicleJourneys();
			context.put(NETEX_LINE_DATA_CONTEXT, vehicleJourneysStruct);
			Parser journeyParser = ParserFactory.create(JourneyParser.class.getName());
			journeyParser.parse(context);
		}
	}

	@Override
	public void initReferentials(Context context) throws Exception {
	}

	private void linkStopPointsToAssignedStopArea(Referential referential) {
        Collection<StopPoint> stopPoints = referential.getStopPoints().values();

        for (StopPoint stopPoint : stopPoints) {
            String stopPointObjectId = stopPoint.getObjectId();
            String stopAreaObjectId;

            if (stopAssignments.containsKey(stopPointObjectId)) {
                stopAreaObjectId = stopAssignments.get(stopPointObjectId);
                StopArea stopArea = ObjectFactory.getStopArea(referential, stopAreaObjectId);
                stopPoint.setContainedInStopArea(stopArea);
                stopPoint.setFilled(true);
            }
        }
    }

	private void sortStopPointsOnRoutes(Referential referential) {
		referential.getRoutes().values().forEach(route -> route.getStopPoints()
				.sort(Comparator.comparing(StopPoint::getPosition)));
	}

	private void updateBoardingAlighting(Referential referential) {

		for (Route route : referential.getRoutes().values()) {
			boolean invalidData = false;
			boolean usefullData = false;

			b1: for (JourneyPattern jp : route.getJourneyPatterns()) {
				for (VehicleJourney vj : jp.getVehicleJourneys()) {
					for (VehicleJourneyAtStop vjas : vj.getVehicleJourneyAtStops()) {
						if (!updateStopPoint(vjas)) {
							invalidData = true;
							break b1;
						}
					}
				}
			}
			if (!invalidData) {
				// check if every stoppoints were updated, complete missing ones to
				// normal; if all normal clean all
				for (StopPoint sp : route.getStopPoints()) {
					if (sp.getForAlighting() == null)
						sp.setForAlighting(AlightingPossibilityEnum.normal);
					if (sp.getForBoarding() == null)
						sp.setForBoarding(BoardingPossibilityEnum.normal);
				}
				for (StopPoint sp : route.getStopPoints()) {
					if (!sp.getForAlighting().equals(AlightingPossibilityEnum.normal)) {
						usefullData = true;
						break;
					}
					if (!sp.getForBoarding().equals(BoardingPossibilityEnum.normal)) {
						usefullData = true;
						break;
					}
				}

			}
			if (invalidData || !usefullData) {
				// remove useless informations
				for (StopPoint sp : route.getStopPoints()) {
					sp.setForAlighting(null);
					sp.setForBoarding(null);
				}
			}

		}
	}

	private boolean updateStopPoint(VehicleJourneyAtStop vjas) {
		StopPoint sp = vjas.getStopPoint();
		BoardingPossibilityEnum forBoarding = getForBoarding(vjas.getBoardingAlightingPossibility());
		AlightingPossibilityEnum forAlighting = getForAlighting(vjas.getBoardingAlightingPossibility());
		if (sp.getForBoarding() != null && !sp.getForBoarding().equals(forBoarding))
			return false;
		if (sp.getForAlighting() != null && !sp.getForAlighting().equals(forAlighting))
			return false;
		sp.setForBoarding(forBoarding);
		sp.setForAlighting(forAlighting);
		return true;
	}

	private AlightingPossibilityEnum getForAlighting(BoardingAlightingPossibilityEnum boardingAlightingPossibility) {
		if (boardingAlightingPossibility == null)
			return AlightingPossibilityEnum.normal;
		switch (boardingAlightingPossibility) {
		case BoardAndAlight:
			return AlightingPossibilityEnum.normal;
		case AlightOnly:
			return AlightingPossibilityEnum.normal;
		case BoardOnly:
			return AlightingPossibilityEnum.forbidden;
		case NeitherBoardOrAlight:
			return AlightingPossibilityEnum.forbidden;
		case BoardAndAlightOnRequest:
			return AlightingPossibilityEnum.request_stop;
		case AlightOnRequest:
			return AlightingPossibilityEnum.request_stop;
		case BoardOnRequest:
			return AlightingPossibilityEnum.normal;
		}
		return null;
	}

	private BoardingPossibilityEnum getForBoarding(BoardingAlightingPossibilityEnum boardingAlightingPossibility) {
		if (boardingAlightingPossibility == null)
			return BoardingPossibilityEnum.normal;
		switch (boardingAlightingPossibility) {
		case BoardAndAlight:
			return BoardingPossibilityEnum.normal;
		case AlightOnly:
			return BoardingPossibilityEnum.forbidden;
		case BoardOnly:
			return BoardingPossibilityEnum.normal;
		case NeitherBoardOrAlight:
			return BoardingPossibilityEnum.forbidden;
		case BoardAndAlightOnRequest:
			return BoardingPossibilityEnum.request_stop;
		case AlightOnRequest:
			return BoardingPossibilityEnum.normal;
		case BoardOnRequest:
			return BoardingPossibilityEnum.request_stop;
		}
		return null;
	}

	static {
		ParserFactory.register(PublicationDeliveryParser.class.getName(), new ParserFactory() {
			private PublicationDeliveryParser instance = new PublicationDeliveryParser();

			@Override
			protected Parser create() {
				return instance;
			}
		});
	}

}
