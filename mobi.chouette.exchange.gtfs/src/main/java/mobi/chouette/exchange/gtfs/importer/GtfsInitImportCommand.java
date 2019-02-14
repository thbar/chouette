package mobi.chouette.exchange.gtfs.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.GtfsTrip;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.validation.ValidationData;
import mobi.chouette.model.util.Referential;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

@Log4j
@Stateless(name = GtfsInitImportCommand.COMMAND)
public class GtfsInitImportCommand implements Command, Constant {

    public static final String COMMAND = "GtfsInitImportCommand";

    @EJB
    private ComparatorTrips comparatorTrips;

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);
            JobData jobData = (JobData) context.get(JOB_DATA);
            context.put(REFERENTIAL, new Referential());
            // prepare importer
            GtfsImporter importer = (GtfsImporter) context.get(PARSER);
            if (importer == null) {
                Path path = Paths.get(jobData.getPathName(), INPUT);
                importer = new GtfsImporter(path.toString());
                // VehicleJourney
                Index<GtfsTrip> gtfsTrips = importer.getTripByRoute();

                Index<GtfsRoute> gtfsRoutes = importer.getRouteById();
                Set<String> gtfsRouteIds = new HashSet<String>();

                for (Iterator<GtfsTrip> gtfsTripIterator = gtfsTrips.iterator(); gtfsTripIterator.hasNext(); ) {
                    GtfsTrip gtfsTrip = gtfsTripIterator.next();
                    gtfsRouteIds.add(gtfsTrip.getRouteId());
                }

                for (String gtfsRouteId : gtfsRouteIds) {
                    for (GtfsTrip gtfsTrip : gtfsTrips.values(gtfsRouteId)) {
                        comparatorTrips.comparatorSequenceTrips(importer, gtfsTrip, configuration);
                    }
                }
                for (GtfsRoute gtfsRouteIdInRoute : gtfsRoutes) {
                    gtfsRouteIdInRoute.setRouteId(gtfsRouteIdInRoute.getRouteId() + "_1");
                }

                context.put(PARSER, importer);
            }
            GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);
            if (parameters.getReferencesType() == null || parameters.getReferencesType().isEmpty()) {
                parameters.setReferencesType("line");
            }
            context.put(VALIDATION_DATA, new ValidationData());
            result = SUCCESS;

        } catch (Exception e) {
            log.error(e, e);
            throw e;
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
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
        CommandFactory.factories.put(GtfsInitImportCommand.class.getName(), new DefaultCommandFactory());
    }

}
