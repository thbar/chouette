package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.*;
import mobi.chouette.exchange.importer.updater.LineOptimiser;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Route;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.util.Referential;
import org.hibernate.Hibernate;
import org.joda.time.LocalDate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.HashSet;

@Log4j
@Stateless(name = DataOverwriteCommand.COMMAND)
public class DataOverwriteCommand implements Command {

    public static final String COMMAND = "DataOverwriteCommand";

    @EJB
    private LineOptimiser optimiser;

    @EJB
    private RouteDAO routeDAO;

    @EJB
    private JourneyPatternDAO journeyPatternDAO;

    @EJB
    private VehicleJourneyDAO vehicleJourneyDAO;

    @Override
    public boolean execute(Context context) throws Exception {

        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);
        ActionReporter reporter = ActionReporter.Factory.getInstance();


        Referential cache = new Referential();
        context.put(CACHE, cache);

        Referential referential = (Referential) context.get(REFERENTIAL);

        optimiser.initialize(cache, referential);

        HashSet<VehicleJourney> vehicleJourneysToDelete = new HashSet<>();

        for(VehicleJourney newVJ : referential.getVehicleJourneys().values()){
            VehicleJourney oldVJ = cache.getVehicleJourneys().get(newVJ.getObjectId());
            if(oldVJ != null && oldVJ.getVehicleJourneyAtStops().size() != 0){
                for(VehicleJourneyAtStop newVehicleJourneyAtStop : newVJ.getVehicleJourneyAtStops()){
                    String objectIdStopInOldVJ = oldVJ.getVehicleJourneyAtStops().get(newVehicleJourneyAtStop.getStopPoint().getPosition()).getStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject().getOriginalStopId();
                    String [] splitObjectIdStopInNewVJ = newVehicleJourneyAtStop.getStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject().getObjectId().split(":");
                    if(!splitObjectIdStopInNewVJ[2].equals(objectIdStopInOldVJ)){
                        LocalDate oldStartDate = oldVJ.getTimetables().get(0).getPeriods().get(0).getStartDate();
                        LocalDate newStartDate = newVJ.getTimetables().get(0).getPeriods().get(0).getStartDate();
                        LocalDate oldEndDate = oldVJ.getTimetables().get(0).getPeriods().get(0).getEndDate();
                        LocalDate newEndDate = newVJ.getTimetables().get(0).getPeriods().get(0).getEndDate();
                        if(oldStartDate.equals(newStartDate) && oldEndDate.equals(newEndDate)) {
                            vehicleJourneysToDelete.add(oldVJ);
                        }
                    }
                }
            }
            result = SUCCESS;
        }

        for (VehicleJourney vehicleJourney : vehicleJourneysToDelete){
//            reporter.addObjectReport(context, vehicleJourney.getObjectId(), ActionReporter.OBJECT_TYPE.VEHICLE_JOURNEY, vehicleJourney.getObjectId() + " -- " + vehicleJourney.getPublishedJourneyName(),
//                    ActionReporter.OBJECT_STATE.WARNING, IO_TYPE.OUTPUT);
            cache.getVehicleJourneys().values().remove(vehicleJourney);
            vehicleJourneyDAO.delete(vehicleJourney);
        }
        vehicleJourneyDAO.flush();


        log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);

        return result;
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
        CommandFactory.factories.put(DataOverwriteCommand.class.getName(), new DefaultCommandFactory());
    }
}
