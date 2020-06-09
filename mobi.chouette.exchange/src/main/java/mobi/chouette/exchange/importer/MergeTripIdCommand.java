package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.Pair;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Stateless(name = MergeTripIdCommand.COMMAND)
@Log4j
public class MergeTripIdCommand implements Command, Constant {

    @EJB
    VehicleJourneyDAO vehicleJourneyDAO;

    public static final String COMMAND = "MergeTripIdCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        List<VehicleJourney> vehicleJourneys = vehicleJourneyDAO.findAll();
        vehicleJourneys.sort(Comparator.comparing(VehicleJourney::getShortTripId));
        final VehicleJourney[] previousVJ = {null};
        ArrayList<Pair<VehicleJourney, VehicleJourney>> vehicleJourneysToDel = new ArrayList<>();
        vehicleJourneys.stream().forEach(currentVJ -> {
            if (previousVJ[0] == null) {
                previousVJ[0] = currentVJ;
            } else {
                if (previousVJ[0].getShortTripId().equalsIgnoreCase(currentVJ.getShortTripId())) {
                    if (shouldRemoveCurrentVJ(previousVJ[0], currentVJ))
                        vehicleJourneysToDel.add(new Pair<>(currentVJ, previousVJ[0]));
                } else {
                    previousVJ[0] = currentVJ;
                }
            }
        });

        updateTimeTablesVehicleJourneysAndRemoveVehicleJourneys(vehicleJourneysToDel);

        return SUCCESS;
    }

    private void updateTimeTablesVehicleJourneysAndRemoveVehicleJourneys(ArrayList<Pair<VehicleJourney, VehicleJourney>> vehicleJourneysToDel) {
        vehicleJourneysToDel.stream().forEach(vehicleJourneyVehicleJourneyPair -> {
            VehicleJourney toDel = vehicleJourneyVehicleJourneyPair.getLeft();
            VehicleJourney toKeep = vehicleJourneyVehicleJourneyPair.getRight();
            toKeep = updateTimetables(toDel, toKeep);
            vehicleJourneyDAO.delete(toDel);
            vehicleJourneyDAO.update(toKeep);
        });
        vehicleJourneyDAO.flush(); // to prevent SQL error outside method
    }

    private VehicleJourney updateTimetables(VehicleJourney toDel, VehicleJourney toKeep){
        toKeep.setObjectId(toKeep.getNewObjectId());
        List<Timetable> newTimetables = toDel.getTimetables();
        List<Timetable> endTimeTables = toKeep.getTimetables();
        newTimetables.stream().forEach(timetable -> {
            if(!endTimeTables.contains(timetable))
                endTimeTables.add(timetable);
        });
        toKeep.setTimetables(endTimeTables);
        return toKeep;
    }

    private boolean shouldRemoveCurrentVJ(VehicleJourney previous, VehicleJourney current){
        final boolean[] keep = {false};
        if(previous.getVehicleJourneyAtStops().size() != current.getVehicleJourneyAtStops().size()) return false;
        if(!previous.getJourneyPattern().equals(current.getJourneyPattern())) return false;
        current.getVehicleJourneyAtStops().stream().forEach(vehicleJourneyAtStop -> {
            if(keep[0] == false) {
                if (previous.getVehicleJourneyAtStops().stream().noneMatch(vehicleJourneyAtStop1 -> vehicleJourneyAtStop1.getStopPoint() == vehicleJourneyAtStop.getStopPoint() &&
                        vehicleJourneyAtStop1.getArrivalTime() == vehicleJourneyAtStop.getArrivalTime()))
                    keep[0] = true;
            }
        });
        return keep[0];
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange/" + COMMAND;
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
        CommandFactory.factories.put(MergeTripIdCommand.class.getName(), new MergeTripIdCommand.DefaultCommandFactory());
    }

}
