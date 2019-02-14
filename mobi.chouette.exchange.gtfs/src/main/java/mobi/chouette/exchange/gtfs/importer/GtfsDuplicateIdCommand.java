package mobi.chouette.exchange.gtfs.importer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.GtfsTrip;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Log4j
@Stateless(name = GtfsDuplicateIdCommand.COMMAND)
public class GtfsDuplicateIdCommand implements Command, Constant {

    public static final String COMMAND = "GtfsDuplicateIdCommand";

    @EJB
    private ComparatorTrips comparatorTrips;

    @Getter
    @Setter
    private String gtfsRouteId;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

        // VehicleJourney
        Index<GtfsTrip> gtfsTrips = importer.getTripByRoute();

        Index<GtfsRoute> gtfsRoutes = importer.getRouteById();
        Set<String> gtfsRouteIds = new HashSet<String>();
        Map<String, Boolean> duplicateIds = new HashMap<>();

        try{
            for(Iterator<GtfsTrip> gtfsTripIterator = gtfsTrips.iterator(); gtfsTripIterator.hasNext();){
                GtfsTrip gtfsTrip = gtfsTripIterator.next();
                gtfsRouteIds.add(gtfsTrip.getRouteId());
            }

            for(String gtfsRouteId : gtfsRouteIds){
                boolean sequenceTripsDifferents = false;
                for (GtfsTrip gtfsTrip : gtfsTrips.values(gtfsRouteId)) {
                    sequenceTripsDifferents = comparatorTrips.comparatorSequenceTrips(importer, gtfsTrip, configuration);
                    duplicateIds.put(gtfsTrip.getTripId(), sequenceTripsDifferents);
                }
            }
            for(GtfsRoute gtfsRouteIdInRoute : gtfsRoutes){
                gtfsRouteIdInRoute.setRouteId(gtfsRouteIdInRoute.getRouteId() + "_1");
            }

            context.put(DUPLICATE_ID, duplicateIds);

            result = SUCCESS;
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        return result;
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
        CommandFactory.factories.put(GtfsDuplicateIdCommand.class.getName(), new DefaultCommandFactory());
    }
}
