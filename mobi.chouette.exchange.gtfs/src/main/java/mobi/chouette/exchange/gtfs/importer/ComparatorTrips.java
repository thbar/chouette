package mobi.chouette.exchange.gtfs.importer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime;
import mobi.chouette.exchange.gtfs.model.GtfsTrip;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.parser.AbstractConverter;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

@Log4j
@Stateless(name = ComparatorTrips.COMMAND)
public class ComparatorTrips {

    public static final String COMMAND = "ComparatorTrips";

    @Getter
    @Setter
    private String gtfsRouteId;

    @EJB
    private VehicleJourneyDAO vehicleJourneyDAO;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean comparatorSequenceTrips(GtfsImporter importer, GtfsTrip gtfsTrip, GtfsImportParameters configuration) {
        // Récupérer les courses correspondantes
        String objectId = AbstractConverter.composeObjectId(configuration,
                VehicleJourney.VEHICLEJOURNEY_KEY, gtfsTrip.getTripId(), log);

        VehicleJourney vehicleJourney = vehicleJourneyDAO.findByObjectId(objectId);

        if(vehicleJourney != null){
            Integer nbStopTime = 0;
            for (GtfsStopTime gtfsStopTime : importer.getStopTimeByTrip().values(gtfsTrip.getTripId())) {
                nbStopTime++;
            }

            if(nbStopTime != vehicleJourney.getVehicleJourneyAtStops().size()){
                return true;
            }
            else{
                // En récupérant l'id des stops comparer l'enchainement des arrêts
                // à terme le faire sur l'ensemble des trips
                for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourney.getVehicleJourneyAtStops()) {
                    for (GtfsStopTime gtfsStopTime : importer.getStopTimeByTrip().values(gtfsTrip.getTripId())) {
                        if (gtfsStopTime.getStopSequence().equals(vehicleJourneyAtStop.getStopPoint().getPosition())) {
                            String stopId = gtfsStopTime.getStopId().trim().replaceAll("[^a-zA-Z_0-9\\-]", "_");
                            String[] parts = vehicleJourneyAtStop.getStopPoint().getObjectId().split("a");
                            String oldStopId = parts[parts.length - 1];
                            if (!stopId.equals(oldStopId)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.gtfs/" + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                // try another way on test context
                String name = "java:module/" + COMMAND;
                try {
                    result = (Command) context.lookup(name);
                } catch (NamingException e1) {
                    log.error(e);
                }
            }
            return result;
        }
    }

    static {
        CommandFactory.factories.put(ComparatorTrips.class.getName(), new DefaultCommandFactory());
    }
}
