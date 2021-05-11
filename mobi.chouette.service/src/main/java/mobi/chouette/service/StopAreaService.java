package mobi.chouette.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.ReferentialDAO;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.stopplace.PublicationDeliveryStopPlaceParser;
import mobi.chouette.exchange.stopplace.StopAreaUpdateContext;
import mobi.chouette.exchange.stopplace.StopAreaUpdateService;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.Referential;
import mobi.chouette.persistence.hibernate.ContextHolder;

@Singleton(name = StopAreaService.BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Log4j
public class StopAreaService {

	public static final String BEAN_NAME = "StopAreaService";

	@EJB(beanName = StopAreaUpdateService.BEAN_NAME)
	private StopAreaUpdateService stopAreaUpdateService;

	@EJB
	private ProviderDAO providerDAO;

	private ExecutorService executor;

	public StopAreaService() {
		final AtomicInteger counter = new AtomicInteger(0);
		ThreadFactory threadFactory = (r) -> {
			Thread t = new Thread(r);
			t.setName("stop-area-reference-updater-thread-" + (counter.incrementAndGet()));
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		};
		int processors = Runtime.getRuntime().availableProcessors();
		executor = Executors.newFixedThreadPool(processors, threadFactory);
		;

	}

	public void createOrUpdateStopPlacesFromNetexStopPlaces(InputStream inputStream) {
		PublicationDeliveryStopPlaceParser parser = new PublicationDeliveryStopPlaceParser(inputStream);

		StopAreaUpdateContext updateContext = parser.getUpdateContext();

		int changedStopCnt = updateContext.getChangedStopCount();
		if (changedStopCnt > 0) {
			log.info("Updating " + changedStopCnt + " stop areas");
			updateSchemas(updateContext);
			log.info("Updated " + changedStopCnt + " stop areas");
		} else {
			log.debug("Received update without any stop areas. Doing nothing");
		}
	}




	/**
	 * Applies stop place modifications on all schemas impacted by modifications
	 *
	 * @param updateContext
	 * 		Context with all modifications to apply
	 */
	private void updateSchemas(StopAreaUpdateContext updateContext ){

		for (String impactedSchema : updateContext.getImpactedSchemas()) {
			log.info("Starting update on schema: " + impactedSchema);
			Context chouetteDbContext = createContext();
			ContextHolder.clear();
			ContextHolder.setContext(impactedSchema);
			stopAreaUpdateService.createOrUpdateStopAreas(chouetteDbContext, updateContext);
			resetSavedStatusToFalse(updateContext);
			log.info("Update completed on schema: " + impactedSchema);
		}

		log.info("Update references started");
		updateStopAreaReferencesPerReferential(updateContext.getMergedQuays());
		log.info("Update references completed");
	}


	/**
	 * Reset all "saved" status to false.
	 * (when all modifications are applied to schema 1, all stopAreas are marked as "saved".
	 * We need to reset this status in order to allow modifications for other schemas)
	 * @param updateContext
	 * 		Context with all modifications that need to be applied.
	 */
	private void resetSavedStatusToFalse(StopAreaUpdateContext updateContext ){

		for (StopArea activeStopArea : updateContext.getActiveStopAreas()) {
			activeStopArea.setSaved(false);
			activeStopArea.getContainedStopAreas().forEach(containedStopArea -> containedStopArea.setSaved(false));
		}
	}

	private void updateStopAreaReferencesPerReferential(Map<String, Set<String>> replacementMap) {
		int updatedStopPointCnt = 0;

		if (!replacementMap.isEmpty()) {
			List<Future<Integer>> futures = new ArrayList();


			List<String> schemaList = providerDAO.getAllWorkingSchemas();

			for (String referential : schemaList) {
				StopAreaUpdateTask updateTask = new StopAreaUpdateTask(referential, replacementMap);
				futures.add(executor.submit(updateTask));
			}
			try {
				for (Future<Integer> future : futures) {
					updatedStopPointCnt += future.get();
				}
			} catch (ExecutionException e) {
				throw new RuntimeException("Exception while updating StopArea references: " + e.getMessage(), e);
			} catch (InterruptedException ie) {
				throw new RuntimeException("Interrupted while waiting for StopArea reference update", ie);
			}
		}


		log.info("Updated stop area references for " + updatedStopPointCnt + " stop points");
	}

	public void deleteStopArea(String objectId) {
		ContextHolder.clear();
		stopAreaUpdateService.deleteStopArea(objectId);
	}

	public void deleteUnusedStopAreas() {
		ContextHolder.clear();
		stopAreaUpdateService.deleteUnusedStopAreas();
	}


	private Context createContext() {
		Context context = new Context();
		Referential referential = new Referential();
		context.put(Constant.REFERENTIAL, referential);
		context.put(Constant.CACHE, referential);
		context.put(Constant.REPORT, new ActionReport());
		context.put(Constant.VALIDATION_REPORT, new ValidationReport());
		return context;
	}


	class StopAreaUpdateTask implements Callable<Integer> {

		private final String referential;
		private final Map<String, Set<String>> replacementMap;

		public StopAreaUpdateTask(String referential, Map<String, Set<String>> replacementMap) {
			this.referential = referential;
			this.replacementMap = replacementMap;
		}

		@Override
		public Integer call() throws Exception {
			ContextHolder.setContext(referential);
			log.debug("Updating stop area references for stop points for referential " + referential);
			int updatedCnt = stopAreaUpdateService.updateStopAreaReferences(replacementMap);
			log.debug("Updated stop area references for " + updatedCnt + " stop points for referential " + referential);
			return updatedCnt;
		}
	}
}
