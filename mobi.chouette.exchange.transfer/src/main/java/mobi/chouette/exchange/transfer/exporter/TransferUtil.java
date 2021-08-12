package mobi.chouette.exchange.transfer.exporter;

import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Route;

import java.util.List;
import java.util.stream.Collectors;

public class TransferUtil {

    /**
     * Checks if route has correct data
     * @param route
     * @return
     * true : route has correct data
     * false: no journey pattern in route OR
     * 				journey patterns with no vehicle journeys OR
     * 				journey patterns deleted
     */
    public static boolean hasRouteData(Route route){

        List<JourneyPattern> undeletedJourneyPatterns = route.getJourneyPatterns().stream()
                .filter(journeyPattern -> !journeyPattern.getSupprime())
                .collect(Collectors.toList());

        if (undeletedJourneyPatterns.isEmpty()){
            return false;
        }

        return undeletedJourneyPatterns.stream()
                .anyMatch(journeyPattern -> journeyPattern.getVehicleJourneys().stream().findAny().isPresent());

    }

}
