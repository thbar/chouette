package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.BikeAccessEnum;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.type.TadEnum;
import mobi.chouette.model.type.WheelchairAccessEnum;
import mobi.chouette.persistence.hibernate.ContextHolder;

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

    @EJB
    VehicleJourneyDAO vehicleJourneyDAO;

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

            manageTAD(vehicleJourneyList, lineToUpdate);
            manageBike(vehicleJourneyList, nbVehicleJourney, lineToUpdate);
            managePMR(vehicleJourneyList, nbVehicleJourney, lineToUpdate);

            lineDAO.update(lineToUpdate);
        });
        lineDAO.flush(); // to prevent SQL error outside method
        vehicleJourneyDAO.flush();

         return SUCCESS;
    }

    private void managePMR(List<VehicleJourney> vehicleJourneyList, long nbVehicleJourney, Line lineToUpdate) {
        // PMR
        if(lineToUpdate.getWheelchairAccess().equals(WheelchairAccessEnum.FULL_ACCESS)){
            vehicleJourneyList.forEach(vehicleJourney -> {
                vehicleJourney.setMobilityRestrictedSuitability(true);
                vehicleJourneyDAO.update(vehicleJourney);
            });
        } else {
            int nbVehicleJourneyWithPMR = (int) vehicleJourneyList.stream()
                    .filter(vehicleJourney -> vehicleJourney.getMobilityRestrictedSuitability() != null && vehicleJourney.getMobilityRestrictedSuitability()).count();
            if (nbVehicleJourneyWithPMR == 0) {
                lineToUpdate.setWheelchairAccess(WheelchairAccessEnum.NO_ACCESS);
            } else if (nbVehicleJourneyWithPMR == nbVehicleJourney) {
                lineToUpdate.setWheelchairAccess(WheelchairAccessEnum.FULL_ACCESS);
            } else {
                lineToUpdate.setWheelchairAccess(WheelchairAccessEnum.PARTIAL_ACCESS);
            }
        }
    }

    private void manageBike(List<VehicleJourney> vehicleJourneyList, long nbVehicleJourney, Line lineToUpdate) {
        // VELOS
        int nbVehicleJourneyWithBike = (int) vehicleJourneyList.stream()
                .filter(vehicleJourney -> vehicleJourney.getBikesAllowed() != null && vehicleJourney.getBikesAllowed()).count();
        if (nbVehicleJourneyWithBike == 0) {
            lineToUpdate.setBike(BikeAccessEnum.NO_ACCESS);
        } else if (nbVehicleJourneyWithBike == nbVehicleJourney) {
            lineToUpdate.setBike(BikeAccessEnum.FULL_ACCESS);
        } else {
            lineToUpdate.setBike(BikeAccessEnum.PARTIAL_ACCESS);
        }
    }

    private void manageTAD(List<VehicleJourney> vehicleJourneyList, Line lineToUpdate) {
        // TAD
        List<VehicleJourneyAtStop> vehicleJourneyAtStopList = vehicleJourneyList.stream()
                .map(VehicleJourney::getVehicleJourneyAtStops)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        int nbVehicleJourneyAtStop = vehicleJourneyAtStopList.size();
        int nbVJASWithTAD = (int) vehicleJourneyAtStopList.stream().filter(this::isVJASTAD).count();
        if (nbVJASWithTAD == 0) {
            lineToUpdate.setTad(TadEnum.NO_TAD);
        } else if (nbVJASWithTAD == nbVehicleJourneyAtStop) {
            lineToUpdate.setTad(TadEnum.FULL_TAD);
        } else {
            lineToUpdate.setTad(TadEnum.PARTIAL_TAD);
        }
    }

    private boolean isVJASTAD(VehicleJourneyAtStop vehicleJourneyAtStop) {
        return vehicleJourneyAtStop.getBoardingAlightingPossibility() != null
                && (vehicleJourneyAtStop.getBoardingAlightingPossibility().equals(BoardingAlightingPossibilityEnum.BoardAndAlightOnRequest)
                || vehicleJourneyAtStop.getBoardingAlightingPossibility().equals(BoardingAlightingPossibilityEnum.AlightOnRequest)
                || vehicleJourneyAtStop.getBoardingAlightingPossibility().equals(BoardingAlightingPossibilityEnum.BoardOnRequest));
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
