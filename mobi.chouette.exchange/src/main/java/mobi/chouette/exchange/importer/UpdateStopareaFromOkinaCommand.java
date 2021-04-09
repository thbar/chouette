package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.MappingHastusZdepDAO;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.ScheduledStopPointDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.updater.IdfmReflexParser;
import mobi.chouette.exchange.importer.updater.NeTExIdfmStopPlaceRegisterUpdater;
import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.model.Provider;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Hibernate;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Log4j
@Stateless(name = UpdateStopareaFromOkinaCommand.COMMAND)
public class UpdateStopareaFromOkinaCommand implements Command {

	public static final String COMMAND = "UpdateStopareaFromOkinaCommand";

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB
	private ScheduledStopPointDAO scheduledStopPointDAO;

	@EJB(beanName = NeTExIdfmStopPlaceRegisterUpdater.BEAN_NAME)
	private NeTExIdfmStopPlaceRegisterUpdater neTExIdfmStopPlaceRegisterUpdater;

	@Resource(lookup = "java:comp/DefaultManagedExecutorService")
	ManagedExecutorService executor;

	@EJB
	private ProviderDAO providerDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean execute(Context context) throws Exception {
		Long stopId = (Long) context.get("stopId");
		// - stop areas maj avec zdep
		stopAreaDAO.flush();
		// - send to tiamat
		StopArea stopArea = stopAreaDAO.find(stopId);
		List<StopArea> areas = new ArrayList<>();
		areas.add(stopArea);

		List<ScheduledStopPoint> scheduledStopPointsContainedInStopArea = scheduledStopPointDAO.getScheduledStopPointsContainedInStopArea(stopArea.getObjectId());
		stopArea.setContainedScheduledStopPoints(scheduledStopPointsContainedInStopArea);
		stopArea.getContainedScheduledStopPoints().forEach(scheduledStopPoint -> Hibernate.initialize(scheduledStopPoint.getStopPoints()));

		List<ScheduledStopPoint> scheduledStopPointsContainedInStopAreaParent = scheduledStopPointDAO.getScheduledStopPointsContainedInStopArea(stopArea.getParent().getObjectId());
		stopArea.getParent().setContainedScheduledStopPoints(scheduledStopPointsContainedInStopAreaParent);
		stopArea.getParent().getContainedScheduledStopPoints().forEach(scheduledStopPoint -> Hibernate.initialize(scheduledStopPoint.getStopPoints()));

		neTExIdfmStopPlaceRegisterUpdater.update(context, areas);

		Command command = CommandFactory.create(new InitialContext(), UpdateMappingZdepZderZdlrAsynchronousCommand.class.getName());
		command.execute(context);

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
		CommandFactory.factories.put(UpdateStopareaFromOkinaCommand.class.getName(), new DefaultCommandFactory());
	}
}
