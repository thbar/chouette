package mobi.chouette.exchange.gtfs.exporter;

import lombok.Data;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.exporter.CompressCommand;
import mobi.chouette.exchange.exporter.SaveMetadataCommand;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j
@Data
public class GtfsExporterProcessingCommands implements ProcessingCommands, Constant {

	public static class DefaultFactory extends ProcessingCommandsFactory {

		@Override
		protected ProcessingCommands create() throws IOException {
			ProcessingCommands result = new GtfsExporterProcessingCommands();
			return result;
		}
	}

	static {
		ProcessingCommandsFactory.factories.put(GtfsExporterProcessingCommands.class.getName(), new DefaultFactory());
	}

	@Override
	public List<? extends Command> getPreProcessingCommands(Context context, boolean withDao) {
		InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
		GtfsExportParameters parameters = (GtfsExportParameters) context.get(CONFIGURATION);
		List<Command> commands = new ArrayList<>();
		try {
			commands.add(CommandFactory.create(initialContext, GtfsInitExportCommand.class.getName()));
			if (parameters.isMappingLinesIds()) {
				commands.add(CommandFactory.create(initialContext, MappingLinesIdsCommand.class.getName()));
			}
		} catch (Exception e) {
			log.error(e, e);
			throw new RuntimeException("unable to call factories");
		}
		return commands;
	}

	@Override
	public List<? extends Command> getLineProcessingCommands(Context context, boolean withDao) {
		InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
		List<Command> commands = new ArrayList<>();
		try {
			initialContext.addToEnvironment(SCHEDULED_STOP_POINTS, context.get(SCHEDULED_STOP_POINTS));
			if (withDao)
				commands.add(CommandFactory.create(initialContext, DaoGtfsLineProducerCommand.class.getName()));
			else
				commands.add(CommandFactory.create(initialContext, GtfsLineProducerCommand.class.getName()));
		} catch (Exception e) {
			log.error(e, e);
			throw new RuntimeException("unable to call factories");
		}

		return commands;

	}

	@Override
	public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao, boolean allSchemas) {
		InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
		GtfsExportParameters parameters = (GtfsExportParameters) context.get(CONFIGURATION);
		List<Command> commands = new ArrayList<>();
		try {
			commands.add(CommandFactory.create(initialContext, DaoGtfsFeedInfoProducerCommand.class.getName()));
//			commands.add(CommandFactory.create(initialContext, GtfsFeedInfoProducerCommand.class.getName()));
			if (!(parameters.getReferencesType().equalsIgnoreCase("stop_area"))) {
				commands.add(CommandFactory.create(initialContext, GtfsSharedDataProducerCommand.class.getName()));
			}
			commands.add(CommandFactory.create(initialContext, GtfsTerminateExportCommand.class.getName()));
			if (!(parameters.getReferencesType().equalsIgnoreCase("stop_area"))) {
				if (parameters.isValidateAfterExport())
					commands.add(CommandFactory.create(initialContext, GtfsValidateExportCommand.class.getName()));
				if (parameters.isAddMetadata())
					commands.add(CommandFactory.create(initialContext, SaveMetadataCommand.class.getName()));
			}
			commands.add(CommandFactory.create(initialContext, CompressCommand.class.getName()));
		} catch (Exception e) {
			log.error(e, e);
			throw new RuntimeException("unable to call factories");
		}
		return commands;
	}

	@Override
	public List<? extends Command> getStopAreaProcessingCommands(Context context, boolean withDao) {
		InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
		List<Command> commands = new ArrayList<>();
		try {
			commands.add(CommandFactory.create(initialContext, GtfsStopAreaProducerCommand.class.getName()));
		} catch (Exception e) {
			log.error(e, e);
			throw new RuntimeException("unable to call factories");
		}

		return commands;
	}

	@Override
	public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao) {
		return new ArrayList<>();
	}

	@Override
	public List<? extends Command> getDisposeCommands(Context context, boolean withDao) {
		List<Command> commands = new ArrayList<>();
		return commands;
	}

	@Override
	public List<? extends Command> getMosaicCommands(Context context, boolean b) {
		return new ArrayList<>();
	}

}
