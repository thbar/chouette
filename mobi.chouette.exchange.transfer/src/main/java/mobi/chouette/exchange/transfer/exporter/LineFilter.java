package mobi.chouette.exchange.transfer.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.*;
import mobi.chouette.model.util.NeptuneUtil;
import org.joda.time.LocalDate;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j
public class LineFilter {

	public boolean filter(Line line, Date startDate, Date endDate) {

		// Clean line
		for (Iterator<Route> routeI = line.getRoutes().iterator(); routeI.hasNext();) {
			Route route = routeI.next();
			if (route.getStopPoints().size() < 2  && !route.getLine().getFlexibleService()) {
				routeI.remove();
				continue;

			}
			for (Iterator<JourneyPattern> jpI = route.getJourneyPatterns().iterator(); jpI.hasNext();) {
				JourneyPattern jp = jpI.next();
				if (jp.getSupprime() || (jp.getStopPoints().size() < 2  && !jp.getRoute().getLine().getFlexibleService())) {
					jpI.remove();
					continue; // no stops
				}
				if (jp.getDepartureStopPoint() == null || jp.getArrivalStopPoint() == null) {
					NeptuneUtil.refreshDepartureArrivals(jp);
				}
				for (Iterator<VehicleJourney> vjI = jp.getVehicleJourneys().iterator(); vjI.hasNext();) {
					VehicleJourney vehicleJourney = vjI.next();
					if (vehicleJourney.getVehicleJourneyAtStops().isEmpty()) {
						vjI.remove();
						continue;
					}
					if (startDate == null && endDate == null) {
						for (Iterator<Timetable> timetableI = vehicleJourney.getTimetables().iterator(); timetableI
								.hasNext();) {

							Timetable timetable = timetableI.next();

							if (timetable.getPeriods().isEmpty() && timetable.getCalendarDays().isEmpty()) {
								timetableI.remove();
							}
						}
						
						if(vehicleJourney.getTimetables().isEmpty()) {
							log.info("Removing VJ with empty timetables: "+ vehicleJourney.getObjectId());
							vjI.remove();
						}
					} else {
						for (Iterator<Timetable> timetableI = vehicleJourney.getTimetables().iterator(); timetableI
								.hasNext();) {

							Timetable timetable = timetableI.next();

							boolean validTimetable = isTimetableValid(timetable, startDate, endDate);
							if (!validTimetable) {
								timetableI.remove();
								continue;
							}
						}
						if(vehicleJourney.getTimetables().isEmpty()) {
							log.info("Removing VJ with no valid timetables: "+ vehicleJourney.getObjectId());
							vjI.remove();
						}
					} // end vehiclejourney loop
				} // end journeyPattern loop
			}
		}

		return line.getRoutes().stream()
				               .anyMatch(this::hasRouteData);
	}

	/**
	 * Checks if route has correct data
	 * @param route
	 * @return
	 * true : route has correct data
	 * false: no journey pattern in route OR
	 * 				journey patterns with no vehicle journeys OR
	 * 				journey patterns deleted
	 */
	private boolean hasRouteData(Route route){

		List<JourneyPattern> undeletedJourneyPatterns = route.getJourneyPatterns().stream()
						            												.filter(journeyPattern -> !journeyPattern.getSupprime())
									                     							.collect(Collectors.toList());

		if (undeletedJourneyPatterns.isEmpty()){
			return false;
		}

		return undeletedJourneyPatterns.stream()
				 					   .anyMatch(journeyPattern -> journeyPattern.getVehicleJourneys().stream().findAny().isPresent());

	}

	private boolean isTimetableValid(Timetable timetable, Date startDate, Date endDate) {
		if (timetable.getPeriods().isEmpty() && timetable.getCalendarDays().isEmpty()) {
			return false;
		}

		if (startDate == null)
			return timetable.isActiveBefore(new LocalDate(endDate));
		else if (endDate == null)
			return timetable.isActiveAfter(new LocalDate(startDate));
		else
			return timetable.isActiveOnPeriod(new LocalDate(startDate), new LocalDate(endDate));

	}

}
