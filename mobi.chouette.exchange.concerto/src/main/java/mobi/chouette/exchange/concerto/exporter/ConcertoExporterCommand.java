package mobi.chouette.exchange.concerto.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.CommandCancelledException;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.ProgressionCommand;
import mobi.chouette.exchange.exporter.AbstractExporterCommand;
import mobi.chouette.exchange.exporter.MergeCommand;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ReportConstant;
import mobi.chouette.persistence.hibernate.ContextHolder;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j
@Stateless(name = ConcertoExporterCommand.COMMAND)
public class ConcertoExporterCommand extends AbstractExporterCommand implements Command, ReportConstant {

	public static final String COMMAND = "ConcertoExporterCommand";

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
		ActionReporter reporter = ActionReporter.Factory.getInstance();

		// initialize reporting and progression
		ProgressionCommand progression = (ProgressionCommand) CommandFactory.create(initialContext,
				ProgressionCommand.class.getName());

		try {
			// read parameters
			Object configuration = context.get(CONFIGURATION);
			if (!(configuration instanceof ConcertoExportParameters)) {
				// fatal wrong parameters
				log.error("invalid parameters for concerto export " + configuration.getClass().getName());
				reporter.setActionError(context, ActionReporter.ERROR_CODE.INVALID_PARAMETERS, "invalid parameters for concerto export " + configuration.getClass().getName());
				return ERROR;
			}

			// init
			ProcessingCommands commands = ProcessingCommandsFactory
					.create(ConcertoExporterProcessingCommands.class.getName());

			//@TODO SCH intégrer keycloak
			List<String> schemas = new ArrayList<>();
			boolean allSchemas;
			if("admin".equals(ContextHolder.getContext())){
				allSchemas = true;
				schemas.add("sqybus");
				schemas.add("perrier");
				schemas.add("mobicitel40");
				schemas.add("mobicite469");
				schemas.add("ctvmi");
				schemas.add("ceobus");
				schemas.add("tvm");
				schemas.add("timbus");
				schemas.add("stile");
			} else {
				allSchemas = false;
				schemas.add(ContextHolder.getContext());
			}
			boolean goodProcessing = false;
			for(String schema : schemas){
				if(!schema.startsWith("mosaic_")) schema = "mosaic_" + schema;
				ContextHolder.setContext(schema);
				goodProcessing = process(context, commands, progression, false,Mode.line, allSchemas);
				if(goodProcessing) result = true;
			}
			if(allSchemas){
				Command mergedCommand = CommandFactory.create(initialContext, MergeCommand.class.getName());
				result = mergedCommand.execute(context);
			}
		} catch (CommandCancelledException e) {
			reporter.setActionError(context, ActionReporter.ERROR_CODE.INTERNAL_ERROR, "Command cancelled");
			log.error(e.getMessage());
		} catch (Exception e) {
			//reporter.setActionError(context, ERROR_CODE.INTERNAL_ERROR,"Fatal :" + e);
			log.error(e.getMessage(), e);
		} finally {
			progression.dispose(context);
			//log.info(Color.YELLOW + monitor.stop() + Color.NORMAL);
		}
		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.concerto/" + COMMAND;
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
		CommandFactory.factories.put(ConcertoExporterCommand.class.getName(), new DefaultCommandFactory());
	}
}
