package mobi.chouette.exchange.concerto.exporter;

import lombok.Data;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.exporter.MergeCommand;
import mobi.chouette.exchange.importer.UpdateMappingZdepZderZdlrCommand;
import mobi.chouette.persistence.hibernate.ContextHolder;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j
@Data
public class ConcertoExporterProcessingCommands implements ProcessingCommands, Constant {

	public static class DefaultFactory extends ProcessingCommandsFactory {

		@Override
		protected ProcessingCommands create() throws IOException {
			ProcessingCommands result = new ConcertoExporterProcessingCommands();
			return result;
		}
	}

	static {
		ProcessingCommandsFactory.factories.put(ConcertoExporterProcessingCommands.class.getName(), new DefaultFactory());
	}

	@Override
	public List<? extends Command> getPreProcessingCommands(Context context, boolean withDao) {
		InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
		List<Command> commands = new ArrayList<>();
		try {
			context.put("ref", ContextHolder.getContext());
			context.put("swallow", Boolean.TRUE);
			commands.add(CommandFactory.create(initialContext, UpdateMappingZdepZderZdlrCommand.class.getName()));
			commands.add(CommandFactory.create(initialContext, ConcertoInitExportCommand.class.getName()));
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
			if (withDao)
				commands.add(CommandFactory.create(initialContext, DaoConcertoLineProducerCommand.class.getName()));
			else
				commands.add(CommandFactory.create(initialContext, ConcertoLineProducerCommand.class.getName()));
		} catch (Exception e) {
			log.error(e, e);
			throw new RuntimeException("unable to call factories");
		}
		return commands;

	}

	@Override
	public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao, boolean allSchemas) {
		InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
		List<Command> commands = new ArrayList<>();
		try {
			commands.add(CommandFactory.create(initialContext, ConcertoSharedDataProducerCommand.class.getName()));
			commands.add(CommandFactory.create(initialContext, ConcertoTerminateExportCommand.class.getName()));
			if(!allSchemas) {
				commands.add(CommandFactory.create(initialContext, MergeCommand.class.getName()));
			}
		} catch (Exception e) {
			log.error(e, e);
			throw new RuntimeException("unable to call factories");
		}

		return commands;
	}

	@Override
	public List<? extends Command> getStopAreaProcessingCommands(Context context, boolean withDao) {
	    //@todo sch Voir pourquoi pas utilisé
        List<Command> commands = new ArrayList<>();
        return commands;
	}

	@Override
	public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao) {
		List<Command> commands = new ArrayList<>();
		return commands;
	}


	@Override
	public List<? extends Command> getDisposeCommands(Context context, boolean withDao) {
        //@todo sch Voir pourquoi pas utilisé
		List<Command> commands = new ArrayList<>();
		return commands;
	}

	@Override
	public List<? extends Command> getMosaicCommands(Context context, boolean b) {
		return new ArrayList<>();
	}

}
