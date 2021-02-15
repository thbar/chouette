package mobi.chouette.exchange.concerto.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.OperatorDAO;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.exchange.CommandCancelledException;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.ProgressionCommand;
import mobi.chouette.exchange.exporter.AbstractExporterCommand;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ProgressionReport;
import mobi.chouette.exchange.report.ReportConstant;
import mobi.chouette.exchange.report.StepProgression;
import mobi.chouette.model.Operator;
import mobi.chouette.model.Provider;
import mobi.chouette.persistence.hibernate.ContextHolder;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j
@Stateless(name = ConcertoExporterCommand.COMMAND)
public class ConcertoExporterCommand extends AbstractExporterCommand implements Command, ReportConstant {

	public static final String COMMAND = "ConcertoExporterCommand";

	@EJB
	ProviderDAO providerDAO;

	@EJB
	OperatorDAO operatorDAO;

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
			boolean goodProcessing;

			List<Provider> all = providerDAO.findAll();
			all.forEach(p -> {
				if(p.isIdfm() && p.getName().toLowerCase().contains("mosaic") && !p.getName().toLowerCase().contains("technique")){
					schemas.add(p.getSchemaName());
				}
			});

			List<String> schemasWithOperator = new ArrayList<>();

			for(String schema : schemas){
				List<Operator> operators = operatorDAO.findByReferential(schema);
				if(operators != null && operators.size() > 0){
					schemasWithOperator.add(schema);
				}
			}

			if(schemasWithOperator.size() == 0){
				log.error("Erreur export Concerto : il n'y a aucun schema avec au moins un operateur");
				reporter.setActionError(context, ActionReporter.ERROR_CODE.NO_DATA_PROCEEDED, "no data exported");
				return ERROR;
			}

			log.info("Lancement de l'export Concerto");

			for(String schema : schemasWithOperator){
				if(!schema.startsWith("mosaic_")) schema = "mosaic_" + schema;
				if(context.containsKey(EXPORTABLE_DATA)){
                    context.put(EXPORTABLE_DATA, new ExportableData());
                }
				ContextHolder.setContext(schema);

				Optional<Provider> provider = providerDAO.findBySchema(schema);
				provider.ifPresent(value -> context.put(OBJECT_TYPE_CONCERTO, value.getObjectTypeConcerto()));
				provider.ifPresent(value -> context.put(PROVIDER, value.getSchemaName()));
				provider.ifPresent(value -> context.put(PERIOD, value.getPeriodConcerto()));

				log.info("Export Concerto, filiale : " + schema);

				if(schema.equals(schemasWithOperator.get(schemasWithOperator.size() - 1))){
					allSchemas = true;
				}
				else{
					allSchemas = false;
				}

				// TODO on réinitalise les étapes déjà passées car la boucle qui passe dans tous les schemas ne permet pas une bonne gestion des étapes actuellement
				// TODO Le action_report est faussé avec seulement les infos de la dernière filiale exportée
				ProgressionReport report = (ProgressionReport) context.get(REPORT);
				report.getProgression().getSteps().get(StepProgression.STEP.INITIALISATION.ordinal()).setRealized(0);
				report.getProgression().getSteps().get(StepProgression.STEP.PROCESSING.ordinal()).setRealized(0);
				report.getProgression().getSteps().get(StepProgression.STEP.FINALISATION.ordinal()).setRealized(0);


				goodProcessing = process(context, commands, progression, false,Mode.line, allSchemas);
				if(goodProcessing) result = true;
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
