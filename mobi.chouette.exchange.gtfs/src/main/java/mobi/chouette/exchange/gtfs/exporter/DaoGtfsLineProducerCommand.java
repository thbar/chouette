/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ConnectionLinkDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.model.Line;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

/**
 *
 */
@Log4j

@Stateless(name = DaoGtfsLineProducerCommand.COMMAND)

public class DaoGtfsLineProducerCommand implements Command, Constant 
{
	public static final String COMMAND = "DaoGtfsLineProducerCommand";

	@Resource
	private SessionContext daoContext;

	@EJB 
	private LineDAO lineDAO;

	@EJB
	private ConnectionLinkDAO connectionLinkDAO;


	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		try {

			Long lineId = (Long) context.get(LINE_ID);
			Line line = lineDAO.find(lineId);
			InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
			initialContext.addToEnvironment(SCHEDULED_STOP_POINTS, context.get(SCHEDULED_STOP_POINTS));
			Command export = CommandFactory.create(initialContext, GtfsLineProducerCommand.class.getName());
			((GtfsLineProducerCommand)export).setConnectionLinkDao(connectionLinkDAO);
			context.put(LINE, line);
			result = export.execute(context);
			//daoContext.setRollbackOnly();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}

		return result;
	}



	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.gtfs/"
						+ COMMAND;
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
		CommandFactory.factories.put(DaoGtfsLineProducerCommand.class.getName(),
				new DefaultCommandFactory());
	}


}
