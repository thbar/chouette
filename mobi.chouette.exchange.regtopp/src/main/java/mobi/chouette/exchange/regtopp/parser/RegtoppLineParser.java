package mobi.chouette.exchange.regtopp.parser;

import static mobi.chouette.common.Constant.CONFIGURATION;
import static mobi.chouette.common.Constant.MAIN_VALIDATION_REPORT;
import static mobi.chouette.common.Constant.PARSER;
import static mobi.chouette.common.Constant.REFERENTIAL;
import static mobi.chouette.exchange.regtopp.Constant.REGTOPP_REPORTER;
import static mobi.chouette.exchange.regtopp.validation.Constant.REGTOPP_FILE_TIX;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.joda.time.Duration;
import org.joda.time.LocalTime;

import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.exchange.regtopp.importer.RegtoppImportParameters;
import mobi.chouette.exchange.regtopp.model.RegtoppDestinationDST;
import mobi.chouette.exchange.regtopp.model.RegtoppFootnoteMRK;
import mobi.chouette.exchange.regtopp.model.RegtoppLineLIN;
import mobi.chouette.exchange.regtopp.model.RegtoppRouteTMS;
import mobi.chouette.exchange.regtopp.model.RegtoppTripIndexTIX;
import mobi.chouette.exchange.regtopp.model.enums.AnnouncementType;
import mobi.chouette.exchange.regtopp.model.enums.DirectionType;
import mobi.chouette.exchange.regtopp.model.enums.TransportType;
import mobi.chouette.exchange.regtopp.model.importer.parser.FileParserValidationError;
import mobi.chouette.exchange.regtopp.model.importer.parser.RegtoppException;
import mobi.chouette.exchange.regtopp.model.importer.parser.RegtoppImporter;
import mobi.chouette.exchange.regtopp.model.importer.parser.index.Index;
import mobi.chouette.exchange.regtopp.validation.RegtoppValidationReporter;
import mobi.chouette.exchange.validation.report.CheckPoint;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.Company;
import mobi.chouette.model.Footnote;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.Period;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.type.DayTypeEnum;
import mobi.chouette.model.type.PTDirectionEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;

@Log4j
public class RegtoppLineParser implements Parser, Validator {

	@Setter
	private String lineId = null;

	/* Validation rules of type I and II are checked during this step,
	 * and results are stored in reports.
	 */
	// TODO. Rename this function "parse(Context context)".
	@Override
	public void validate(Context context) throws Exception {

		// Konsistenssjekker, kjøres før parse-metode.

		// Det som kan sjekkes her er at antall poster stemmer og at alle referanser til andre filer er gyldige

		RegtoppImporter importer = (RegtoppImporter) context.get(PARSER);
		RegtoppValidationReporter validationReporter = (RegtoppValidationReporter) context.get(REGTOPP_REPORTER);
		validationReporter.getExceptions().clear();

		ValidationReport mainReporter = (ValidationReport) context.get(MAIN_VALIDATION_REPORT);

		mainReporter.getCheckPoints().add(new CheckPoint(REGTOPP_FILE_TIX, CheckPoint.RESULT.UNCHECK, CheckPoint.SEVERITY.ERROR));

		if (importer.hasTIXImporter()) {
			validationReporter.reportSuccess(context, REGTOPP_FILE_TIX, RegtoppTripIndexTIX.FILE_EXTENSION);

			Index<RegtoppTripIndexTIX> index = importer.getTripIndex();

			if (index.getLength() == 0) {
				FileParserValidationError fileError = new FileParserValidationError(RegtoppTripIndexTIX.FILE_EXTENSION, 0, null,
						RegtoppException.ERROR.FILE_WITH_NO_ENTRY, null, "Empty file");
				validationReporter.reportError(context, new RegtoppException(fileError), RegtoppTripIndexTIX.FILE_EXTENSION);
			}

			for (RegtoppTripIndexTIX bean : index) {
				try {
					// Call index validator
					index.validate(bean, importer);
				} catch (Exception ex) {
					if (ex instanceof RegtoppException) {
						validationReporter.reportError(context, (RegtoppException) ex, RegtoppTripIndexTIX.FILE_EXTENSION);
					} else {
						validationReporter.throwUnknownError(context, ex, RegtoppTripIndexTIX.FILE_EXTENSION);
					}
				}
			validationReporter.reportErrors(context, bean.getErrors(), RegtoppTripIndexTIX.FILE_EXTENSION);
			validationReporter.validate(context, RegtoppTripIndexTIX.FILE_EXTENSION, bean.getOkTests());			}
		}

	}

	/*
	 * Validation rules of type III are checked at this step.
	 */
	// TODO. Rename this function "translate(Context context)" or "produce(Context context)", ...
	@Override
	public void parse(Context context) throws Exception {

		// Her tar vi allerede konsistenssjekkede data (ref validate-metode over) og bygger opp tilsvarende struktur i chouette.
		// Merk at import er linje-sentrisk, så man skal i denne klassen returnerer 1 line med x antall routes og stoppesteder, journeypatterns osv

		Referential referential = (Referential) context.get(REFERENTIAL);

		// Clear any previous data as this referential is reused / TODO
		if (referential != null) {
			referential.clear(true);
		}

		RegtoppImporter importer = (RegtoppImporter) context.get(PARSER);
		RegtoppImportParameters configuration = (RegtoppImportParameters) context.get(CONFIGURATION);

		String chouetteLineId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), Line.LINE_KEY, lineId, log);

		// Create the actual Chouette Line and put it in the "referential" space (which is later used by the LineImporterCommand)
		Line line = ObjectFactory.getLine(referential, chouetteLineId);

		// Find line number (TODO check if index exists)
		Index<RegtoppLineLIN> lineById = importer.getLineById();
		RegtoppLineLIN regtoppLine = lineById.getValue(lineId);
		if (regtoppLine != null) {
			line.setName(regtoppLine.getName());
			line.setPublishedName(regtoppLine.getName());
		}
		

		Index<RegtoppDestinationDST> destinationIndex = importer.getDestinationById();

		// Add routes and journey patterns
		Index<RegtoppRouteTMS> routeIndex = importer.getRouteIndex();

		for (RegtoppRouteTMS routeSegment : routeIndex) {
			if (lineId.equals(routeSegment.getLineId())) {

				// Add network
				String chouetteNetworkId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.PTNETWORK_KEY,
						routeSegment.getAdminCode(), log);
				Network ptNetwork = ObjectFactory.getPTNetwork(referential, chouetteNetworkId);
				if (!ptNetwork.isFilled()) {
					ptNetwork.setSourceIdentifier("Regtopp");
					ptNetwork.setName(routeSegment.getAdminCode());
					ptNetwork.setRegistrationNumber(routeSegment.getAdminCode());
					ptNetwork.setFilled(true);
				}
				line.setNetwork(ptNetwork);
				

				// Add authority company
				String chouetteCompanyId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.COMPANY_KEY,
						routeSegment.getAdminCode(), log);
				Company company = ObjectFactory.getCompany(referential, chouetteCompanyId);
				if (!company.isFilled()) {
					company.setRegistrationNumber(routeSegment.getAdminCode());
					company.setName("Authority " + routeSegment.getAdminCode());
					company.setCode(routeSegment.getAdminCode());
					company.setFilled(true);
				}
				line.setCompany(company);

				String routeKey = routeSegment.getLineId() + routeSegment.getDirection() + routeSegment.getRouteId();

				// Create route
				String chouetteRouteId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.ROUTE_KEY, routeKey, log);
				Route route = ObjectFactory.getRoute(referential, chouetteRouteId);
				if (!route.isFilled()) {
					// Filled = only a flag to indicate that we no longer should write data to this entity
					RegtoppDestinationDST arrivalText = destinationIndex.getValue(routeSegment.getDestinationId());
					if (arrivalText != null) {
						route.setName(arrivalText.getDestinationText());
					}
					
					route.setDirection(routeSegment.getDirection() == DirectionType.Outbound ? PTDirectionEnum.A : PTDirectionEnum.R);
					
					// TODO UNSURE
					route.setNumber(routeSegment.getRouteId());
					route.setLine(line);
					route.setFilled(true);
				}

				// Create journey pattern
				String chouetteJourneyPatternId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.JOURNEYPATTERN_KEY,
						routeKey, log);

				JourneyPattern journeyPattern = ObjectFactory.getJourneyPattern(referential, chouetteJourneyPatternId);
				journeyPattern.setRoute(route);

				// Create stop point
				String chouetteStopPointId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.STOPPOINT_KEY,
						routeKey + routeSegment.getSequenceNumberStop(), log);

				StopPoint stopPoint = createStopPoint(referential, context, routeSegment, chouetteStopPointId);

				// Add stop point to journey pattern AND route (for now)
				journeyPattern.addStopPoint(stopPoint);
				route.getStopPoints().add(stopPoint);

			}
		}
		// Sort stopPoints on JourneyPattern
		Collection<JourneyPattern> journeyPatterns = referential.getJourneyPatterns().values();
		for (JourneyPattern jp : journeyPatterns) {
			List<StopPoint> stopPoints = jp.getStopPoints();
			Collections.sort(stopPoints, new Comparator<StopPoint>() {

				@Override
				public int compare(StopPoint arg0, StopPoint arg1) {
					return arg0.getPosition().compareTo(arg1.getPosition());
				}
			});
			jp.setDepartureStopPoint(stopPoints.get(0));
			jp.setArrivalStopPoint(stopPoints.get(stopPoints.size() - 1));
		}

		// Sort stopPoints on route
		Collection<Route> routes = referential.getRoutes().values();
		for (Route r : routes) {
			List<StopPoint> stopPoints = r.getStopPoints();
			Collections.sort(stopPoints, new Comparator<StopPoint>() {
				@Override
				public int compare(StopPoint arg0, StopPoint arg1) {
					return arg0.getPosition().compareTo(arg1.getPosition());
				}
			});
		}

		// TODO Loop over routes and link outbound/inbound routes together

		// Add VehicleJourneys
		Index<RegtoppTripIndexTIX> tripIndex = importer.getTripIndex();
		for (RegtoppTripIndexTIX trip : tripIndex) {
			if (trip.getLineId().equals(lineId)) {
				if (trip.getNotificationType() == AnnouncementType.Announced) {

					// This is where we get the line number
					line.setNumber(trip.getLineNumberVisible());

					String tripKey = trip.getLineId() + trip.getTripId();
					String routeKey = trip.getLineId() + trip.getDirection() + trip.getRouteIdRef();

					String chouetteVehicleJourneyId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.VEHICLEJOURNEY_KEY,
							tripKey, log);
					VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential, chouetteVehicleJourneyId);

					// Add authority company
					String chouetteOperatorId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.COMPANY_KEY,
							trip.getOperatorCode(), log);
					Company operator = ObjectFactory.getCompany(referential, chouetteOperatorId);
					operator.setRegistrationNumber(trip.getOperatorCode());
					operator.setName("Operator " + trip.getOperatorCode());
					operator.setCode(trip.getOperatorCode());
					vehicleJourney.setCompany(operator);

					addFootnote(trip.getFootnoteId1Ref(), vehicleJourney, line, importer);
					addFootnote(trip.getFootnoteId2Ref(), vehicleJourney, line,importer);

					RegtoppDestinationDST arrivalText = destinationIndex.getValue(trip.getDestinationIdArrivalRef());

					// TODO unsure
					if (arrivalText != null) {
						vehicleJourney.setPublishedJourneyName(arrivalText.getDestinationText());
					}

					vehicleJourney.setPublishedJourneyIdentifier(StringUtils.trimToNull(trip.getLineNumberVisible()));
					TransportType typeOfService = trip.getTypeOfService();
					TransportModeNameEnum transportMode = convertTypeOfService(typeOfService);
					vehicleJourney.setTransportMode(transportMode);

					String chouetteRouteId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.ROUTE_KEY, routeKey, log);
					Route route = ObjectFactory.getRoute(referential, chouetteRouteId);

					String chouetteJourneyPatternId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.JOURNEYPATTERN_KEY,
							routeKey, log);
					JourneyPattern journeyPattern = ObjectFactory.getJourneyPattern(referential, chouetteJourneyPatternId);

					vehicleJourney.setJourneyPattern(journeyPattern);
					vehicleJourney.setRoute(route);

					// Duration since midnight
					// Link to timetable
					String chouetteTimetableId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.TIMETABLE_KEY,
							trip.getDayCodeRef(), log);

					Duration tripDepartureTime = trip.getDepartureTime();
					if(tripDepartureTime.getStandardSeconds() >= 24*60*60) {
						// After midnight 
						chouetteTimetableId +=RegtoppTimetableParser.AFTER_MIDNIGHT_SUFFIX;
					}
					Timetable timetable = ObjectFactory.getTimetable(referential, chouetteTimetableId);
					timetable.addVehicleJourney(vehicleJourney);
					

					// TODO this must be precomputed instead of iterating over tens of thousands of records for each trip.
					for (RegtoppRouteTMS vehicleStop : importer.getRouteIndex()) {
						if (vehicleStop.getLineId().equals(lineId)) {
							if (vehicleStop.getRouteId().equals(trip.getRouteIdRef())) {
								if (vehicleStop.getDirection() == trip.getDirection()) {

									VehicleJourneyAtStop vehicleJourneyAtStop = ObjectFactory.getVehicleJourneyAtStop();
									vehicleJourneyAtStop.setVehicleJourney(vehicleJourney);

									// Default = board and alight
									vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardAndAlight);
									
									// TODO verify this
									if(vehicleStop.getDriverTimeArrival() != null) {
										vehicleJourneyAtStop.setArrivalTime(calculateTripVisitTime(tripDepartureTime, vehicleStop.getDriverTimeArrival()));
									} else {
										vehicleJourneyAtStop.setArrivalTime(calculateTripVisitTime(tripDepartureTime, vehicleStop.getDriverTimeDeparture()));
										vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardOnly);
									}
									
									if(vehicleStop.getDriverTimeDeparture() != null) {
										vehicleJourneyAtStop.setDepartureTime(calculateTripVisitTime(tripDepartureTime, vehicleStop.getDriverTimeDeparture()));
									} else {
										vehicleJourneyAtStop.setDepartureTime(calculateTripVisitTime(tripDepartureTime, vehicleStop.getDriverTimeArrival()));
										vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.AlightOnly);
									}

									String chouetteStopPointId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(),
											ObjectIdTypes.STOPPOINT_KEY, routeKey + vehicleStop.getSequenceNumberStop(), log);

									StopPoint stopPoint = ObjectFactory.getStopPoint(referential, chouetteStopPointId);
									vehicleJourneyAtStop.setStopPoint(stopPoint);

								}
							}
						}
					}

				} else {
					log.info("Skipping unannouced trip: " + trip);
				}
			}
		}
		
		for(Route route : referential.getRoutes().values()) {
			if(route.getName() == null) {
				// TODO check if Route has name
			}
			
			route.setPublishedName(route.getName());

			for(JourneyPattern jp : route.getJourneyPatterns()) {
				jp.setName(route.getName());
			}
		}

	}
	
	public static Time calculateTripVisitTime(Duration tripDepartureTime, Duration timeSinceTripDepatureTime){
		// TODO Ugly ugly ugly 
		
		// TODO handle hours after midnight
		LocalTime localTime = new LocalTime(0,0,0,0).plusSeconds((int)( tripDepartureTime.getStandardSeconds()+timeSinceTripDepatureTime.getStandardSeconds()));
		
		java.sql.Time sqlTime = new java.sql.Time(localTime.getHourOfDay(), localTime.getMinuteOfHour(), localTime.getSecondOfMinute());
		
		return sqlTime;
		
	}

	private TransportModeNameEnum convertTypeOfService(TransportType typeOfService) {
		switch (typeOfService) {
		case AirplaneOrAirportExpress:
			return TransportModeNameEnum.RapidTransit;
		case ExpressCoach:
			return TransportModeNameEnum.Coach;
		case FerryBoat:
			return TransportModeNameEnum.Ferry;
		case LocalBus:
			return TransportModeNameEnum.Bus;
		case Subway:
			return TransportModeNameEnum.Metro;
		case Train:
			return TransportModeNameEnum.LocalTrain;
		case Tram:
			return TransportModeNameEnum.Tramway;
		case Various:
			return TransportModeNameEnum.Other;
		default:
			return TransportModeNameEnum.Other;
		}
	}

	private StopPoint createStopPoint(Referential referential, Context context, RegtoppRouteTMS routeSegment, String chouetteStopPointId) throws Exception {

		RegtoppImporter importer = (RegtoppImporter) context.get(PARSER);
		RegtoppImportParameters configuration = (RegtoppImportParameters) context.get(CONFIGURATION);

		StopPoint stopPoint = ObjectFactory.getStopPoint(referential, chouetteStopPointId);
		stopPoint.setPosition(Integer.parseInt(routeSegment.getSequenceNumberStop()));

		String chouetteStopAreaId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), ObjectIdTypes.STOPAREA_KEY, routeSegment.getStopId(),
				log);

		StopArea stopArea = ObjectFactory.getStopArea(referential, chouetteStopAreaId);

		stopPoint.setContainedInStopArea(stopArea);

		return stopPoint;
	}

	private void addFootnote(String footnoteId, VehicleJourney vehicleJourney, Line line, RegtoppImporter importer) throws Exception {
		if (!"000".equals(footnoteId)) {

			Index<RegtoppFootnoteMRK> index = importer.getFootnoteById();
			RegtoppFootnoteMRK footnote = index.getValue(footnoteId);

			if (footnote != null) {
				
				Footnote f = new Footnote();
				
				f.setLabel(footnote.getDescription());
				f.setKey(footnote.getFootnoteId());
				f.setCode(footnote.getFootnoteId());

				// TODO footnotes does not persist in database. 
				vehicleJourney.getFootnotes().add(f);
				f.setLine(line);
				line.getFootnotes().add(f);
				
				
			} else {
				// TODO report correctly
				log.warn("Invalid footnote id " + footnoteId);
			}
		}
	}
	

	static {
		ParserFactory.register(RegtoppLineParser.class.getName(), new ParserFactory() {
			@Override
			protected Parser create() {
				return new RegtoppLineParser();
			}
		});
	}

}
