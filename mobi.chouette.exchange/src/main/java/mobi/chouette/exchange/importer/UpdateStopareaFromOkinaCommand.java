package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ScheduledStopPointDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.updater.NeTExIdfmStopPlaceRegisterUpdater;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.Referential;
import org.hibernate.Hibernate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j
@Stateless(name = UpdateStopareaFromOkinaCommand.COMMAND)
public class UpdateStopareaFromOkinaCommand implements Command {

	public static final String COMMAND = "UpdateStopareaFromOkinaCommand";

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB
	ScheduledStopPointDAO scheduledStopPointDAO;

	@EJB(beanName = NeTExIdfmStopPlaceRegisterUpdater.BEAN_NAME)
	private NeTExIdfmStopPlaceRegisterUpdater neTExIdfmStopPlaceRegisterUpdater;

	@Override
	//@TransactionAttribute(TransactionAttributeType.REQUIRED)
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


		Referential referential = (Referential) context.get(REFERENTIAL);

		neTExIdfmStopPlaceRegisterUpdater.update(context, referential, areas);
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
