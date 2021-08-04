package mobi.chouette.exchange.neptune.analyzeFile;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.ProgressionCommand;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.neptune.Constant;
import mobi.chouette.exchange.neptune.importer.NeptuneImportParameters;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.model.StopArea;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;

@Log4j
public class NeptuneAnalyzeFileCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "NeptuneAnalyzeFileCommand";


    @Override
    public boolean execute(Context context) throws Exception {

        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        ActionReport report = (ActionReport) context.get(REPORT);
        context.put(INCOMING_LINE_LIST, new ArrayList());
        ProgressionCommand progression = (ProgressionCommand) CommandFactory.create(initialContext, ProgressionCommand.class.getName());
        ActionReporter reporter = ActionReporter.Factory.getInstance();

        AnalyzeReport analyzeReport = new AnalyzeReport();
        context.put(ANALYSIS_REPORT, analyzeReport);

        // check params
        Object configuration = context.get(CONFIGURATION);
        if (!(configuration instanceof NeptuneImportParameters)) {
            reporter.setActionError(context, ActionReporter.ERROR_CODE.INVALID_PARAMETERS,"invalid parameters for gtfs import " + configuration.getClass().getName());
            return ERROR;
        }

        NeptuneImportParameters parameters = (NeptuneImportParameters) configuration;
        context.put(StopArea.IMPORT_MODE, parameters.getStopAreaImportMode());
        ProcessingCommands commands = ProcessingCommandsFactory.create(NeptuneAnalyzeFileProcessingCommands.class.getName());

        try{
            result = process(context, commands, progression, true, Mode.line);
            log.info("Neptune analysis completed");
            report.setResult("OK");
            progression.saveAnalyzeReport(context,true);
        }catch(Exception e){
            report.setResult("NOK");
            reporter.setActionError(context, ActionReporter.ERROR_CODE.INTERNAL_ERROR,"Fatal :" + e);
        }

        return result;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NeptuneAnalyzeFileCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NeptuneAnalyzeFileCommand.class.getName(), new NeptuneAnalyzeFileCommand.DefaultCommandFactory());
    }
}
