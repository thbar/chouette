package mobi.chouette.exchange.neptune.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.core.CoreException;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.ConnectionLinkRegisterBlocCommand;
import mobi.chouette.exchange.neptune.Constant;
import mobi.chouette.exchange.neptune.validation.AbstractValidator;
import mobi.chouette.exchange.neptune.validation.AccessLinkValidator;
import mobi.chouette.exchange.neptune.validation.AccessPointValidator;
import mobi.chouette.exchange.neptune.validation.AreaCentroidValidator;
import mobi.chouette.exchange.neptune.validation.ChouetteRouteValidator;
import mobi.chouette.exchange.neptune.validation.CompanyValidator;
import mobi.chouette.exchange.neptune.validation.ConnectionLinkValidator;
import mobi.chouette.exchange.neptune.validation.GroupOfLineValidator;
import mobi.chouette.exchange.neptune.validation.ITLValidator;
import mobi.chouette.exchange.neptune.validation.JourneyPatternValidator;
import mobi.chouette.exchange.neptune.validation.LineValidator;
import mobi.chouette.exchange.neptune.validation.PTNetworkValidator;
import mobi.chouette.exchange.neptune.validation.PtLinkValidator;
import mobi.chouette.exchange.neptune.validation.StopAreaValidator;
import mobi.chouette.exchange.neptune.validation.StopPointValidator;
import mobi.chouette.exchange.neptune.validation.TimeSlotValidator;
import mobi.chouette.exchange.neptune.validation.TimetableValidator;
import mobi.chouette.exchange.neptune.validation.VehicleJourneyValidator;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.exchange.validation.ValidatorFactory;
import mobi.chouette.model.Line;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = NeptuneStopAreaMergeCommand.COMMAND)
public class NeptuneStopAreaMergeCommand implements Command, Constant {

	public static final String COMMAND = "NeptuneStopAreaMergeCommand";

	@EJB
	private StopAreaDAO stopAreaDAO;

	private Map<String, List<StopArea>> stopAreasByOriginalID = new HashMap<>();

	boolean result = SUCCESS;

	@Override
	public boolean execute(Context context) throws Exception {


		Monitor monitor = MonitorFactory.start(COMMAND);

		ActionReporter reporter = ActionReporter.Factory.getInstance();
		List<StopArea> stopAreaList = stopAreaDAO.findAll();
		mergeCommercialPoints(stopAreaList);
		stopAreasByOriginalID.clear();
		mergeQuays(stopAreaList);

		return result;
	}

	private void mergeCommercialPoints(List<StopArea> stopAreaList ){
		stopAreaList.stream().filter(stopArea->stopArea.getOriginalStopId() != null && ChouetteAreaEnum.CommercialStopPoint.equals(stopArea.getAreaType()))
				.forEach(this::addStopAreaToMap);

		mergeDuplicateStopAreas();
	}

	private void mergeDuplicateStopAreas(){
		stopAreasByOriginalID.entrySet().stream()
				.filter(entry -> entry.getValue().size() > 1)
				.forEach(this::mergeStopAreas);
	}

	private void mergeQuays(List<StopArea> stopAreaList ){
		stopAreaList.stream().filter(stopArea->stopArea.getOriginalStopId() != null && ChouetteAreaEnum.BoardingPosition.equals(stopArea.getAreaType()))
							  .forEach(this::addStopAreaToMap);

		mergeDuplicateStopAreas();
	}

	private void mergeStopAreas(Map.Entry<String,List<StopArea>> entry) {

			String originalStopId = entry.getKey();

			List<Long> idsToMerge = entry.getValue().stream()
												.map(StopArea::getId)
												.sorted()
												.collect(Collectors.toList());

			Long referenceId = idsToMerge.get(0);

			for (int i = 1 ; i< idsToMerge.size() ; i++){
				Long idToMerge = idsToMerge.get(i);
				log.warn("Merging StopArea :"+idToMerge+" to stopArea:"+referenceId+ " ,originalStopId:"+originalStopId);
				try {
					stopAreaDAO.mergeStopArea30m(idToMerge,referenceId);
					// TODO Après avoir mergé 2 stopAreas, il faut avertir TIAMAT pour qu'il se mette à jour
				} catch (CoreException e) {
					log.error("Error while merging StopArea :"+idToMerge+" to stopArea:"+referenceId+ " , originalStopId:"+originalStopId);
					result = ERROR;
				}try {
					stopAreaDAO.mergeStopArea30m(idToMerge,referenceId);
					// TODO Après avoir mergé 2 stopAreas, il faut avertir TIAMAT pour qu'il se mette à jour
				} catch (CoreException e) {
					log.error("Error while merging StopArea :"+idToMerge+" to stopArea:"+referenceId+ " , originalStopId:"+originalStopId);
					result = ERROR;
				}
			}
	}

	private void addStopAreaToMap(StopArea stopArea){
		String originalStopId = stopArea.getOriginalStopId();
		List<StopArea> stopAreaList = stopAreasByOriginalID.get(originalStopId);
		if (stopAreaList == null){
			stopAreaList = new ArrayList<>();
			stopAreasByOriginalID.put(originalStopId,stopAreaList);
		}
		stopAreaList.add(stopArea);
	}


	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.neptune/" + COMMAND;
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
		CommandFactory.factories.put(NeptuneStopAreaMergeCommand.class.getName(), new DefaultCommandFactory());
	}
}
