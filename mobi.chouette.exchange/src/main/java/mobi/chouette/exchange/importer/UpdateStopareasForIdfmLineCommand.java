package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.MappingHastusZdepDAO;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.ScheduledStopPointDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.dao.StopPointDAO;
import mobi.chouette.exchange.importer.updater.IdfmReflexParser;
import mobi.chouette.exchange.importer.updater.NeTExIdfmStopPlaceRegisterUpdater;
import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.model.Provider;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.Referential;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
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

	@EJB
	private ProviderDAO providerDAO;

	@EJB
	ScheduledStopPointDAO scheduledStopPointDAO;

	@EJB
	MappingHastusZdepDAO mappingHastusZdepDAO;

	@EJB(beanName = NeTExIdfmStopPlaceRegisterUpdater.BEAN_NAME)
	private NeTExIdfmStopPlaceRegisterUpdater neTExIdfmStopPlaceRegisterUpdater;

	@Resource(lookup = "java:comp/DefaultManagedExecutorService")
	ManagedExecutorService executor;

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

			areas.forEach(sa -> {
				List<ScheduledStopPoint> scheduledStopPointsContainedInStopArea = scheduledStopPointDAO.getScheduledStopPointsContainedInStopArea(sa.getObjectId());
				sa.setContainedScheduledStopPoints(scheduledStopPointsContainedInStopArea);
				sa.getContainedScheduledStopPoints().forEach(scheduledStopPoint -> Hibernate.initialize(scheduledStopPoint.getStopPoints()));

				List<ScheduledStopPoint> scheduledStopPointsContainedInStopAreaParent = scheduledStopPointDAO.getScheduledStopPointsContainedInStopArea(sa.getParent().getObjectId());
				sa.getParent().setContainedScheduledStopPoints(scheduledStopPointsContainedInStopAreaParent);
				sa.getParent().getContainedScheduledStopPoints().forEach(scheduledStopPoint -> Hibernate.initialize(scheduledStopPoint.getStopPoints()));
			});


            Referential referential = (Referential) context.get(REFERENTIAL);
			neTExIdfmStopPlaceRegisterUpdater.update(context, referential, areas);


			//maj des zdlr
			CommandCallable callable = new CommandCallable();
			Optional<Provider> provider = providerDAO.findBySchema(ContextHolder.getContext());
			callable.codeIdfm = provider.orElseThrow(() -> new RuntimeException("Aucun provider trouvé avec pour schema " + referential)).getCodeIdfm();
			callable.context = ContextHolder.getContext();
			Future<Void> future = executor.submit(callable);
			future.isDone();

			return SUCCESS;
		} catch (Exception e){
			if(e.getMessage().contains("MOSAIC_SQL_ERROR:")){
				String[] splitErrors = e.getMessage().split("MOSAIC_SQL_ERROR:");
				context.put("MOSAIC_SQL_ERROR", splitErrors[1]);
			}
			throw new Exception(e.getMessage());
		}
	}

	private class CommandCallable implements Callable<Void> {
		private String codeIdfm;
		private String context;

		@Override
		@TransactionAttribute(TransactionAttributeType.REQUIRED)
		public Void call() throws Exception {
			ContextHolder.setContext(this.context);
			String requestHttpTarget = String.format(System.getProperty("iev.stop.place.zdep.zder.zdlr.mapping.by.ref"), this.codeIdfm);
			InputStream input = new ByteArrayInputStream(PublicationDeliveryReflexService.getAll(requestHttpTarget));
			HashMap<String, Pair<String, String>> stringPairHashMap = IdfmReflexParser.parseReflexResult(input);

			stringPairHashMap.forEach((zdep, zderZdlrPair) -> {
				Optional<MappingHastusZdep> byZdep = mappingHastusZdepDAO.findByZdep(zdep);
				if (byZdep.isPresent()) {
					MappingHastusZdep mappingHastusZdep = byZdep.get();
					mappingHastusZdep.setZder(zderZdlrPair.getLeft());
					mappingHastusZdep.setZdlr(zderZdlrPair.getRight());
					mappingHastusZdepDAO.update(mappingHastusZdep);
				}
			});

			return null;
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
