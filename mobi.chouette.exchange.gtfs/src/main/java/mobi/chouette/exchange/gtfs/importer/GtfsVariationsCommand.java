package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.CommandCancelledException;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.ProgressionCommand;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.report.ActionReporter;

import javax.naming.InitialContext;
import java.io.IOException;

@Log4j
public class GtfsVariationsCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "GtfsVariationsCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);

        ProgressionCommand progression = (ProgressionCommand) CommandFactory.create(initialContext,
                ProgressionCommand.class.getName());
        ActionReporter reporter = ActionReporter.Factory.getInstance();
        try {
            // check params
            Object configuration = context.get(CONFIGURATION);
            if (!(configuration instanceof GtfsImportParameters)) {
                // fatal wrong parameters
                //log.error("invalid parameters for gtfs import " + configuration.getClass().getName());
                reporter.setActionError(context, ActionReporter.ERROR_CODE.INVALID_PARAMETERS,"invalid parameters for gtfs import " + configuration.getClass().getName());
                return ERROR;
            }

            GtfsImportParameters parameters = (GtfsImportParameters) configuration;
            // import total par défaut
            if (parameters.getReferencesType() == null) parameters.setReferencesType("line");
            boolean all = !(parameters.getReferencesType().equalsIgnoreCase("stop_area"));

            ProcessingCommands commands = ProcessingCommandsFactory.create(GtfsVariationsProcessingCommand.class.getName());
            result = process(context, commands, progression, true, (all?Mode.line:Mode.stopareas));

        } catch (CommandCancelledException e) {
            reporter.setActionError(context, ActionReporter.ERROR_CODE.INTERNAL_ERROR, "Command cancelled");
            log.error(e.getMessage());
        } catch (Exception e) {
            // log.error(e.getMessage(), e);
            reporter.setActionError(context, ActionReporter.ERROR_CODE.INTERNAL_ERROR,"Fatal :" + e);
        } finally {
            progression.dispose(context);
            log.info(Color.YELLOW + monitor.stop() + Color.NORMAL);
        }


        return result;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new GtfsVariationsCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(GtfsVariationsCommand.class.getName(), new DefaultCommandFactory());
    }
}
