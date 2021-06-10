package mobi.chouette.exchange.gtfs.analyzeFile;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.ProgressionCommand;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.AnalyzeReport;

import javax.naming.InitialContext;
import java.io.IOException;

@Log4j
public class GtfsAnalyzeFileCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "GtfsAnalyzeFileCommand";


    @Override
    public boolean execute(Context context) throws Exception {

        boolean result = ERROR;
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        ActionReport report = (ActionReport) context.get(REPORT);
        ProgressionCommand progression = (ProgressionCommand) CommandFactory.create(initialContext, ProgressionCommand.class.getName());
        ActionReporter reporter = ActionReporter.Factory.getInstance();

        AnalyzeReport analyzeReport = new AnalyzeReport();
        context.put(ANALYSIS_REPORT, analyzeReport);

        // check params
        Object configuration = context.get(CONFIGURATION);
        if (!(configuration instanceof GtfsImportParameters)) {
            reporter.setActionError(context, ActionReporter.ERROR_CODE.INVALID_PARAMETERS,"invalid parameters for gtfs import " + configuration.getClass().getName());
            return ERROR;
        }

        ProcessingCommands commands = ProcessingCommandsFactory.create(GtfsAnalyzeFileProcessingCommands.class.getName());

        try{
            result = process(context, commands, progression, true, Mode.line);
            report.setResult("OK");
            progression.saveAnalyzeReport(context,true);
            log.info("Gtfs analysis completed");
        }catch(Exception e){
            report.setResult("NOK");
        }
        return true;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new GtfsAnalyzeFileCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(GtfsAnalyzeFileCommand.class.getName(), new GtfsAnalyzeFileCommand.DefaultCommandFactory());
    }
}
