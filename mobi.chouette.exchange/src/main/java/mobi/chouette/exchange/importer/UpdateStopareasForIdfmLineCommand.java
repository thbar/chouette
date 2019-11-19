package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.dao.StopPointDAO;
import mobi.chouette.exchange.importer.updater.NeTExIdfmStopPlaceRegisterUpdater;
import mobi.chouette.model.Line;
import mobi.chouette.model.NeptuneIdentifiedObject;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.util.Referential;
import org.hibernate.Hibernate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = UpdateStopareasForIdfmLineCommand.COMMAND)
public class UpdateStopareasForIdfmLineCommand implements Command {

	public static final String COMMAND = "UpdateStopareasForIdfmLineCommand";

	@EJB
	private LineDAO lineDAO;

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB
	private StopPointDAO stopPointDAO;

	@EJB(beanName = NeTExIdfmStopPlaceRegisterUpdater.BEAN_NAME)
	private NeTExIdfmStopPlaceRegisterUpdater neTExIdfmStopPlaceRegisterUpdater;

	@Override
	//@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean execute(Context context) throws Exception {
		try {
			Long lineId = (Long) context.get("lineId");
			// - stop areas maj avec zdep
			String updatedStopArea = lineDAO.updateStopareasForIdfmLineCommand(lineId);
			lineDAO.flush();
			// - send to tiamat
			List<Long> idList = Arrays.asList(updatedStopArea.split("-")).stream().map(Long::parseLong).collect(Collectors.toList());
			List<StopArea> areas = stopAreaDAO.findAll(idList);
			areas.forEach(sa -> Hibernate.initialize(sa.getParent()));

            Referential referential = (Referential) context.get(REFERENTIAL);
            referential = initRefential(referential, areas);

			neTExIdfmStopPlaceRegisterUpdater.update(context, referential, areas);
			return SUCCESS;
		} catch (Exception e){
			throw new Exception(e.getCause());
		}
	}

	public Referential initRefential(Referential referential, List<StopArea> areas){
		referential.setLines(lineDAO.findAll().stream().collect(Collectors.toMap(Line::getObjectId, l -> l)));

		referential.setStopAreas(areas.stream().collect(Collectors.toMap(StopArea::getObjectId, l -> l)));
		referential.setSharedStopAreas(areas
				.stream()
				.filter(sa -> sa.getParent() != null)
				.map(StopArea::getParent)
				.collect(Collectors.toMap(sa -> sa.getId().toString(), sa -> sa)));

		referential.setStopPoints(stopPointDAO.findAll().stream().collect(Collectors.toMap(StopPoint::getObjectId, l -> l)));

		referential.getSharedStopAreas().forEach((k, area) -> {
			Hibernate.initialize(area.getContainedScheduledStopPoints());
			area.getContainedScheduledStopPoints().forEach(sp -> Hibernate.initialize(sp.getStopPoints()));
		});
		return referential;
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
