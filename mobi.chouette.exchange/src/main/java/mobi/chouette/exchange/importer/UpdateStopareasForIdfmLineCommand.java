package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;


@Log4j
@Stateless(name = UpdateStopareasForIdfmLineCommand.COMMAND)
public class UpdateStopareasForIdfmLineCommand implements Command {

	public static final String COMMAND = "UpdateStopareasForIdfmLineCommand";

	@EJB
	private LineDAO lineDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean execute(Context context) throws Exception {
		try {
			boolean result = ERROR;
			String referential = (String) context.get("referential");
			Long lineId = (Long) context.get("lineId");
			lineDAO.updateStopareasForIdfmLineCommand(referential, lineId);
			result = SUCCESS;
			return result;
		} catch (Exception e){
			throw new Exception(e.getCause());
		}
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
		CommandFactory.factories.put(UpdateStopareasForIdfmLineCommand.class.getName(), new DefaultCommandFactory());
	}
}
