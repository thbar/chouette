package mobi.chouette.exchange.gtfs.importer;


import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Chain;
import mobi.chouette.common.chain.ChainCommand;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.VariationsDAO;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.importer.UncompressCommand;
import mobi.chouette.exchange.importer.updater.StopAreaIdMapper;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;


@Log4j
@Stateless(name = GtfsVariationsProcessingCommand.COMMAND)
public class GtfsVariationsProcessingCommand implements Command {

    public static final String COMMAND = "GtfsVariationsProcessingCommand";

    @EJB
    VariationsDAO variationsDAO;

    @EJB(beanName = StopAreaIdMapper.BEAN_NAME)
    StopAreaIdMapper stopAreaIdMapper;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean execute(Context context) throws Exception {

        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);


        try {
            ArrayList<Command> commands = new ArrayList<>();
            commands.add(CommandFactory.create(initialContext, UncompressCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, GtfsValidationRulesCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, GtfsInitImportCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, GtfsValidationCommand.class.getName()));
            GtfsImporter importer = (GtfsImporter) context.get(PARSER);
            Index<GtfsRoute> index = importer.getRouteById();


            for (GtfsRoute gtfsRoute : index) {
                commands.add(CommandFactory.create(initialContext, GtfsStopParserCommand.class.getName()));
            }


        } catch (Exception ex) {
            log.error(ex.getMessage());
        } finally {
            log.info(Color.RED + monitor.stop() + Color.NORMAL);
        }


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
        CommandFactory.factories.put(GtfsVariationsProcessingCommand.class.getName(), new DefaultCommandFactory());
    }
}

