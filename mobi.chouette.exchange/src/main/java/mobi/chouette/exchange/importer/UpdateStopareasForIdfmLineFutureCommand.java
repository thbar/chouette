package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.updater.NeTExIdfmStopPlaceRegisterUpdater;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = UpdateStopareasForIdfmLineFutureCommand.COMMAND)
public class UpdateStopareasForIdfmLineFutureCommand implements Command {

	public static final String COMMAND = "UpdateStopareasForIdfmLineFutureCommand";

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB(beanName = NeTExIdfmStopPlaceRegisterUpdater.BEAN_NAME)
	private NeTExIdfmStopPlaceRegisterUpdater neTExIdfmStopPlaceRegisterUpdater;

	@Override
	//@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean execute(Context context) throws Exception {

		Referential future_referential = (Referential) context.get("future_referential");
		List<StopArea> future_areas = (List<StopArea>) context.get("future_areas");

		try {
			// Pour initialiser la SESSION et le lazyloading des areas
			List<StopArea> areas = future_areas.stream().map(stopArea -> stopAreaDAO.find(stopArea.getId())).collect(Collectors.toList());
			neTExIdfmStopPlaceRegisterUpdater.update(context, future_referential, areas);
		} catch (Exception e){
			return ERROR;
		}
		return SUCCESS;
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
		CommandFactory.factories.put(UpdateStopareasForIdfmLineFutureCommand.class.getName(), new DefaultCommandFactory());
	}
}
