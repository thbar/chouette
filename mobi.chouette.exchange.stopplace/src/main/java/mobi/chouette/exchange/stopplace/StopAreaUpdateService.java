package mobi.chouette.exchange.stopplace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.ScheduledStopPointDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.updater.StopAreaUpdater;
import mobi.chouette.exchange.importer.updater.Updater;
import mobi.chouette.exchange.netexprofile.importer.util.StopPlaceRegistryIdFetcher;
import mobi.chouette.model.StopArea;
import org.hibernate.Hibernate;

@Singleton(name = StopAreaUpdateService.BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Log4j
public class StopAreaUpdateService {

	private static final int DELETE_UNUSED_BATCH_SIZE = 1000;

	public static final String BEAN_NAME = "StopAreaUpdateService";

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB(beanName = StopAreaUpdater.BEAN_NAME)
	private Updater<StopArea> stopAreaUpdater;


	@EJB
	private ScheduledStopPointDAO scheduledStopPointDAO;

	@EJB
	StopPlaceRegistryIdFetcher stopPlaceRegistryIdFetcher;



	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void createOrUpdateStopAreas(Context context, StopAreaUpdateContext updateContext) {
		new StopAreaUpdateTask(stopAreaDAO, stopAreaUpdater, context, updateContext).update();
	}

	@TransactionAttribute
	public void deleteStopArea(String objectId) {
		StopArea stopArea = stopAreaDAO.findByObjectId(objectId);
		if (stopArea != null) {
			cascadeDeleteStopArea(stopArea);
		} else {
			log.info("Ignored delete for unknown stop area: " + objectId);
		}
	}

	/**
	 * Update stop area references in seperate transaction in order to iterate over all referentials
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public int updateStopAreaReferences(Map<String, Set<String>> replacementMap) {
		final AtomicInteger updatedStopPoints = new AtomicInteger();
		replacementMap.forEach((newStopAreaId, oldStopAreaIds) -> updatedStopPoints.addAndGet(scheduledStopPointDAO.replaceContainedInStopAreaReferences(oldStopAreaIds, newStopAreaId)));
		return updatedStopPoints.get();
	}


	private void cascadeDeleteStopArea(StopArea stopArea) {
		stopArea.getContainedStopAreas().forEach(child -> cascadeDeleteStopArea(child));
		stopAreaDAO.delete(stopArea);
		log.info("Deleted stop area: " + stopArea.getObjectId());
	}

	public void setStopPlaceRegistryIdFetcher(StopPlaceRegistryIdFetcher stopPlaceRegistryIdFetcher) {
		this.stopPlaceRegistryIdFetcher = stopPlaceRegistryIdFetcher;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Integer delete() {

		Set<String> boardingPositionObjectIds = new HashSet<>(stopAreaDAO.getBoardingPositionObjectIds());

		log.debug("Total no of boarding positions: " + boardingPositionObjectIds.size());

		boardingPositionObjectIds.removeAll(stopPlaceRegistryIdFetcher.getQuayIds());

		log.debug("No of boarding positions not in Stop Place Registry: " + boardingPositionObjectIds.size());


		List<String> inUseBoardingPositionsForReferential = scheduledStopPointDAO.getAllStopAreaObjectIds();
		boardingPositionObjectIds.removeAll(inUseBoardingPositionsForReferential);
		log.debug("Removed: " + inUseBoardingPositionsForReferential.size() + " in use boarding positions.Potentially not used boarding positions left: " + boardingPositionObjectIds.size());


		final AtomicInteger deletedStopAreasCnt = new AtomicInteger();

		if (boardingPositionObjectIds.size() > 0) {
			log.info("Found " + boardingPositionObjectIds.size() + " unused boarding positions. Deleting boarding positions and commercial stops where all boarding positions are unused");
			if (boardingPositionObjectIds.size() > DELETE_UNUSED_BATCH_SIZE) {
				Lists.partition(new ArrayList<>(boardingPositionObjectIds), DELETE_UNUSED_BATCH_SIZE).forEach(batch -> deletedStopAreasCnt.addAndGet(deleteBatchOfUnusedStopAreas(batch, boardingPositionObjectIds)));
			} else {
				deletedStopAreasCnt.addAndGet(deleteBatchOfUnusedStopAreas(boardingPositionObjectIds, boardingPositionObjectIds));
			}
		}
		log.info("Finished deleting unused stop areas. Cnt: " + deletedStopAreasCnt.get());
		return deletedStopAreasCnt.get();
	}

	private int deleteBatchOfUnusedStopAreas(Collection<String> unusedBoardingPositionObjectIdBatch, Set<String> allUnusedBoardingPositionObjectIds) {
		Set<StopArea> unusedBoardingPositions = new HashSet<>(stopAreaDAO.findByObjectId(unusedBoardingPositionObjectIdBatch));

		List<StopArea> unusedStopAreas = unusedBoardingPositions.stream()
				.map(boardingPosition -> boardingPosition.getParent())
				.distinct()
				.filter(stop -> stop != null)
				.filter(stop -> stop.getContainedStopAreas().stream().allMatch(boardingPosition -> allUnusedBoardingPositionObjectIds.contains(boardingPosition.getObjectId())))
				.peek(stop -> log.debug("Deleting unused stop area: " + stop)).collect(Collectors.toList());

		unusedBoardingPositions.stream().peek(boardingPosition -> boardingPosition.setParent(null)).forEach(boardingPosition -> stopAreaDAO.delete(boardingPosition));
		unusedStopAreas.forEach(stop -> stopAreaDAO.delete(stop));

		return unusedStopAreas.size() + unusedBoardingPositions.size();
	}
}
