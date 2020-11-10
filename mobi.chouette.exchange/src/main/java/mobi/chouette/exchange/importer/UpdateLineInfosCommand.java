package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.BikeAccessEnum;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.type.TadEnum;
import mobi.chouette.model.type.WheelchairAccessEnum;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Stateless(name = UpdateLineInfosCommand.COMMAND)
@Log4j
public class UpdateLineInfosCommand implements Command, Constant {

    @EJB
    LineDAO lineDAO;

    public static final String COMMAND = "UpdateLineInfosCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        lineDAO.findAll().forEach(line -> {
            List<VehicleJourney> vehicleJourneyList = line.getRoutes().stream()
                    .map((Route::getJourneyPatterns))
                    .flatMap(List::stream)
                    .map(JourneyPattern::getVehicleJourneys)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            long nbVehicleJourney = vehicleJourneyList.size();
            Line lineToUpdate = lineDAO.find(line.getId());

            // TAD
            List<VehicleJourneyAtStop> vehicleJourneyAtStopList = vehicleJourneyList.stream()
                    .map(VehicleJourney::getVehicleJourneyAtStops)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            int nbVehicleJourneyAtStop = vehicleJourneyAtStopList.size();
            int nbVJASWithTAD = vehicleJourneyAtStopList.stream()
                    .filter(vehicleJourneyAtStop -> isVJASTAD(vehicleJourneyAtStop))
                    .collect(Collectors.toList())
                    .size();
            if (nbVJASWithTAD == 0) {
                lineToUpdate.setTad(TadEnum.NO_TAD);
            } else if (nbVJASWithTAD == nbVehicleJourneyAtStop) {
                lineToUpdate.setTad(TadEnum.FULL_TAD);
            } else {
                lineToUpdate.setTad(TadEnum.PARTIAL_TAD);
            }

            // VELOS
            int nbVehicleJourneyWithBike = vehicleJourneyList.stream()
                    .filter(vehicleJourney -> vehicleJourney.getBikesAllowed() != null && vehicleJourney.getBikesAllowed().booleanValue())
                    .collect(Collectors.toList())
                    .size();
            if (nbVehicleJourneyWithBike == 0) {
                lineToUpdate.setBike(BikeAccessEnum.NO_ACCESS);
            } else if (nbVehicleJourneyWithBike == nbVehicleJourney) {
                lineToUpdate.setBike(BikeAccessEnum.FULL_ACCESS);
            } else {
                lineToUpdate.setBike(BikeAccessEnum.PARTIAL_ACCESS);
            }

            // PMR
            int nbVehicleJourneyWithPMR = vehicleJourneyList.stream()
                    .filter(vehicleJourney -> vehicleJourney.getMobilityRestrictedSuitability() != null && vehicleJourney.getMobilityRestrictedSuitability().booleanValue())
                    .collect(Collectors.toList())
                    .size();
            if (nbVehicleJourneyWithPMR == 0) {
                lineToUpdate.setWheelchairAccess(WheelchairAccessEnum.NO_ACCESS);
            } else if (nbVehicleJourneyWithBike == nbVehicleJourney) {
                lineToUpdate.setWheelchairAccess(WheelchairAccessEnum.FULL_ACCESS);
            } else {
                lineToUpdate.setWheelchairAccess(WheelchairAccessEnum.PARTIAL_ACCESS);
            }

            lineDAO.update(lineToUpdate);
        });
        lineDAO.flush(); // to prevent SQL error outside method

        return SUCCESS;
    }

    private boolean isVJASTAD(VehicleJourneyAtStop vehicleJourneyAtStop) {
        if(vehicleJourneyAtStop.getBoardingAlightingPossibility() != null
                && (vehicleJourneyAtStop.getBoardingAlightingPossibility().equals(BoardingAlightingPossibilityEnum.BoardAndAlightOnRequest)
                || vehicleJourneyAtStop.getBoardingAlightingPossibility().equals(BoardingAlightingPossibilityEnum.AlightOnRequest)
                || vehicleJourneyAtStop.getBoardingAlightingPossibility().equals(BoardingAlightingPossibilityEnum.BoardOnRequest)))
            return true;
        return false;
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
        CommandFactory.factories.put(UpdateLineInfosCommand.class.getName(), new UpdateLineInfosCommand.DefaultCommandFactory());
    }

}
