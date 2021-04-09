package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.MappingHastusZdepDAO;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.updater.NeTExIdfmStopPlaceRegisterUpdater;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.model.ObjectReference;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopArea;
import org.hibernate.Hibernate;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
	private MappingHastusZdepDAO mappingHastusZdepDAO;

	@EJB(beanName = NeTExIdfmStopPlaceRegisterUpdater.BEAN_NAME)
	private NeTExIdfmStopPlaceRegisterUpdater neTExIdfmStopPlaceRegisterUpdater;


	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean execute(Context context) throws Exception {
		try {
			Long lineId = (Long) context.get(LINEID);

			// - stop areas maj avec zdep
			Line line = lineDAO.find(lineId);
			if(line.getCategoriesForLine() == null || !line.getCategoriesForLine().getName().equalsIgnoreCase("IDFM")) {
				return SUCCESS;
			}

			log.info("MAJ ZDEP des PA de la ligne :" + line.getObjectId());
			Hibernate.initialize(line.getRoutes());
			line.getRoutes().forEach(route -> {
				Hibernate.initialize(route.getStopPoints());
				route.getStopPoints().forEach(stopPoint -> {
					Hibernate.initialize(stopPoint.getScheduledStopPoint());
					Hibernate.initialize(stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject());
				});
			});
			List<StopArea> areasTosend = new ArrayList<StopArea>();
			List<StopArea> areas = line.getRoutes().stream()
					.map(Route::getJourneyPatterns)
					.flatMap(List::stream)
					.map(JourneyPattern::getStopPoints)
					.flatMap(List::stream)
					.map(stopPoint -> stopPoint.getScheduledStopPoint().getContainedInStopAreaRef())
					.map(ObjectReference::getObject)
					.filter(stopArea -> stopArea.getMappingHastusZdep() == null)
					.distinct()
					.collect(Collectors.toList());


			log.info("MAJ ZDEP nombre de PA : " + areas.size());

			for (StopArea stopArea : areas) {
				Optional<MappingHastusZdep> byHastus = mappingHastusZdepDAO.findByHastus(stopArea.getOriginalStopId());
				byHastus.ifPresent(mappingHastusZdep -> {
					log.info("MAJ ZDEP : " + mappingHastusZdep.getZdep() + " du PA : " + stopArea.getOriginalStopId());
					mappingHastusZdep.setHastusChouette(stopArea.getOriginalStopId());
					stopArea.setMappingHastusZdep(mappingHastusZdep);
					stopAreaDAO.update(stopArea);
					areasTosend.add(stopArea);
				});
			}
			stopAreaDAO.flush();
			log.info("Fin MAJ ZDEP");

			List<StopArea> areasTiamat = areasTosend.stream().map(stopArea -> stopAreaDAO.find(stopArea.getId())).collect(Collectors.toList());
			log.info("Envoi ZDEP vers Tiamat : " + areasTiamat.size() + " PA");
			neTExIdfmStopPlaceRegisterUpdater.update(context, areasTiamat);

			return SUCCESS;
		} catch (Exception e){
			if(e.getMessage().contains("MOSAIC_SQL_ERROR:")){
				String[] splitErrors = e.getMessage().split("MOSAIC_SQL_ERROR:");
				context.put("MOSAIC_SQL_ERROR", splitErrors[1]);
			}
			throw new Exception(e.getMessage());
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
