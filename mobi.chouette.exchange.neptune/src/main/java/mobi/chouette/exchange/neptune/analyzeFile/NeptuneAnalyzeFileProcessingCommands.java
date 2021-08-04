package mobi.chouette.exchange.neptune.analyzeFile;

import lombok.Data;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Chain;
import mobi.chouette.common.chain.ChainCommand;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.fileAnalysis.ProcessAnalyzeCommand;
import mobi.chouette.exchange.importer.CleanRepositoryCommand;
import mobi.chouette.exchange.importer.UncompressCommand;
import mobi.chouette.exchange.neptune.importer.NeptuneBrokenRouteFixerCommand;
import mobi.chouette.exchange.neptune.importer.NeptuneImportExtensionsCommand;
import mobi.chouette.exchange.neptune.importer.NeptuneImportParameters;
import mobi.chouette.exchange.neptune.importer.NeptuneInitImportCommand;
import mobi.chouette.exchange.neptune.importer.NeptuneParserCommand;
import mobi.chouette.exchange.neptune.importer.NeptuneSAXParserCommand;
import mobi.chouette.exchange.neptune.importer.NeptuneSetDefaultValuesCommand;
import mobi.chouette.exchange.neptune.importer.NeptuneTimeTablePeriodFixerCommand;
import mobi.chouette.exchange.neptune.importer.NeptuneValidationCommand;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;


import javax.naming.InitialContext;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Data
@Log4j
public class NeptuneAnalyzeFileProcessingCommands implements ProcessingCommands, Constant {

    public static class DefaultFactory extends ProcessingCommandsFactory {

        @Override
        protected ProcessingCommands create() throws IOException {
            ProcessingCommands result = new NeptuneAnalyzeFileProcessingCommands();
            return result;
        }
    }

    static {
        ProcessingCommandsFactory.factories.put(NeptuneAnalyzeFileProcessingCommands.class.getName(), new DefaultFactory());
    }

    @Override
    public List<? extends Command> getPreProcessingCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        NeptuneImportParameters parameters = (NeptuneImportParameters) context.get(CONFIGURATION);
        List<Command> commands = new ArrayList<>();
        try {
            if (withDao && parameters.isCleanRepository()) {
                commands.add(CommandFactory.create(initialContext, CleanRepositoryCommand.class.getName()));
            }
            commands.add(CommandFactory.create(initialContext, UncompressCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, NeptuneInitImportCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, NeptuneTimeTablePeriodFixerCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, NeptuneBrokenRouteFixerCommand.class.getName()));
        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }
        return commands;
    }

    @Override
    public List<? extends Command> getLineProcessingCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        NeptuneImportParameters parameters = (NeptuneImportParameters) context.get(CONFIGURATION);
        ActionReporter reporter = ActionReporter.Factory.getInstance();
        boolean level3validation = context.get(VALIDATION) != null;
        List<Command> commands = new ArrayList<>();
        JobData jobData = (JobData) context.get(JOB_DATA);
        Path path = Paths.get(jobData.getPathName(), INPUT);
        try {
            List<Path> excluded = FileUtil.listFiles(path, "*", "*.xml");
            if (!excluded.isEmpty()) {
                for (Path exclude : excluded) {
                    reporter.setFileState(context, exclude.getFileName().toString(), IO_TYPE.INPUT, ActionReporter.FILE_STATE.IGNORED);
                }
            }
            List<Path> stream = FileUtil.listFiles(path, "*.xml", "*metadata*");
            for (Path file : stream) {
                Chain chain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());
                commands.add(chain);
                // validation schema
                String url = file.toUri().toURL().toExternalForm();
                NeptuneSAXParserCommand schema = (NeptuneSAXParserCommand) CommandFactory.create(initialContext,
                        NeptuneSAXParserCommand.class.getName());
                schema.setFileURL(url);
                chain.add(schema);

                // parser
                NeptuneParserCommand parser = (NeptuneParserCommand) CommandFactory.create(initialContext,
                        NeptuneParserCommand.class.getName());
                parser.setFileURL(file.toUri().toURL().toExternalForm());
                chain.add(parser);

                // extensions
                NeptuneImportExtensionsCommand extension = (NeptuneImportExtensionsCommand) CommandFactory.create(initialContext,
                        NeptuneImportExtensionsCommand.class.getName());
                chain.add(extension);

                // validation
                Command validation = CommandFactory.create(initialContext, NeptuneValidationCommand.class.getName());
                chain.add(validation);

                // default values
                Command defaults = CommandFactory.create(initialContext, NeptuneSetDefaultValuesCommand.class.getName());
                chain.add(defaults);

                // analyze
                Command analyze = CommandFactory.create(initialContext, ProcessAnalyzeCommand.class.getName());
                chain.add(analyze);

            }

        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }

        return commands;
    }

    @Override
    public List<? extends Command> getStopAreaProcessingCommands(Context context, boolean withDao) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao) {
        List<Command> commands = new ArrayList<>();
        return commands;
    }

    @Override
    public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao, boolean allSchemas) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Command> getDisposeCommands(Context context, boolean withDao) {
        return  new ArrayList<>();
    }

    @Override
    public List<? extends Command> getMobiitiCommands(Context context, boolean b) {
        return new ArrayList<>();
    }

}
