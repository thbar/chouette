/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.concerto.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.concerto.Constant;
import mobi.chouette.exchange.concerto.exporter.producer.ConcertoOperatorProducer;
import mobi.chouette.exchange.concerto.exporter.producer.ConcertoStopProducer;
import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import mobi.chouette.exchange.concerto.model.ConcertoStopAreaZdepObjectIdGenerator;
import mobi.chouette.exchange.concerto.model.ConcertoStopAreaZderObjectIdGenerator;
import mobi.chouette.exchange.concerto.model.ConcertoStopAreaZdlrObjectIdGenerator;
import mobi.chouette.exchange.concerto.model.StopAreaTypeEnum;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Line;
import mobi.chouette.model.Operator;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import org.joda.time.LocalDate;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 */
@Log4j
public class ConcertoSharedDataProducerCommand implements Command, Constant {
	public static final String COMMAND = "ConcertoSharedDataProducerCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);
		ActionReporter reporter = ActionReporter.Factory.getInstance();

		try {

			ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
			if (collection == null) {
				return ERROR;
			}

			saveData(context);
			reporter.addObjectReport(context, "merged", OBJECT_TYPE.COMPANY, "companies", OBJECT_STATE.OK,
					IO_TYPE.OUTPUT);
			reporter.addObjectReport(context, "merged", OBJECT_TYPE.STOP_AREA, "stop areas", OBJECT_STATE.OK,
					IO_TYPE.OUTPUT);
			reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.STOP_AREA, OBJECT_TYPE.STOP_AREA, collection
					.getCommercialStops().size() + collection.getPhysicalStops().size());
			result = SUCCESS;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}

		return result;
	}

	private void saveData(Context context) {
		ConcertoExporter exporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
		ConcertoExportParameters parameters = (ConcertoExportParameters) context.get(CONFIGURATION);
		ConcertoStopProducer stopProducer = new ConcertoStopProducer(exporter);
		ConcertoOperatorProducer operatorProducer = new ConcertoOperatorProducer(exporter);
		ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
		List<MappingLineUUID> mappingLineUUIDList = (List<MappingLineUUID>)context.get(MAPPING_LINE_UUID);
		List<StopPoint> stopPointList = collection.getAllParsedStopPoints();

		LocalDate startDate;
		if (parameters.getStartDate() != null) {
			startDate = LocalDate.fromDateFields(parameters.getStartDate());
		} else {
			startDate = new LocalDate();
		}

		LocalDate endDate;
		if (parameters.getEndDate() != null) {
			endDate = LocalDate.fromDateFields(parameters.getEndDate());
		} else if (parameters.getPeriodDays() != null) {
			endDate = startDate.plusDays(parameters.getPeriodDays());
		} else {
			endDate = startDate.plusDays(30);
		}

		Set<StopArea> physicalStops = collection.getPhysicalStops();
		List<StopArea> zderStops = new ArrayList<>();
		List<StopArea> zdlrStops = new ArrayList<>();
		MapperLinesAndZone mapperLinesAndZders = new MapperLinesAndZone();
		MapperLinesAndZone mapperLinesAndZdlrs = new MapperLinesAndZone();

		// no lambda : esprit chouette
		// zdep
		for (StopArea stop : physicalStops) {
			try {
				if (stop.getMappingHastusZdep() == null) continue;
				UUID[] lineIdArray = new UUID[0];
				StopArea stopZder = createStopZder(stop, zderStops);
				lineIdArray = getLineUuids(mappingLineUUIDList, stopPointList, stop, lineIdArray);
				zderStops = addZderStopIfNotExists(zderStops, stopZder);
				mapperLinesAndZders.addMappingZoneLines(stop.getMappingHastusZdep().getZder(), lineIdArray);
				mapperLinesAndZdlrs.addMappingZoneLines(stop.getMappingHastusZdep().getZdlr(), lineIdArray);
				ConcertoObjectId objectId = ConcertoStopAreaZdepObjectIdGenerator.getConcertoObjectId(stop);
				stopProducer.save(stop, null, stopZder, startDate, endDate, objectId, lineIdArray, StopAreaTypeEnum.ZDEP);
			} catch (Exception e){
				throw e;
			}
		}

		//zder
		for (StopArea stop : zderStops) {
			if(stop.getMappingHastusZdep() == null) continue;
			UUID[] lineIdArray = mapperLinesAndZders.getLinesForZone(stop.getMappingHastusZdep().getZder());
			StopArea stopZdlr = createStopZdlr(stop, zdlrStops);
			zdlrStops = addZdlrStopIfNotExists(zdlrStops, stopZdlr);
			ConcertoObjectId objectId = ConcertoStopAreaZderObjectIdGenerator.getConcertoObjectId(stop);
			stopProducer.save(stop, stopZdlr, null, startDate, endDate, objectId, lineIdArray, StopAreaTypeEnum.ZDER);
		}

		//zdlr
		for (StopArea stop : zdlrStops) {
			if(stop.getMappingHastusZdep() == null) continue;
			if(!stop.getIsExternal().booleanValue()) continue;
			UUID[] lineIdArray = mapperLinesAndZdlrs.getLinesForZone(stop.getMappingHastusZdep().getZdlr());
			ConcertoObjectId objectId = ConcertoStopAreaZdlrObjectIdGenerator.getConcertoObjectId(stop);
			stopProducer.save(stop, null, null, startDate, endDate, objectId, lineIdArray, StopAreaTypeEnum.ZDLR);
		}

		List<Operator> operators = (List<Operator>) context.get(EXPORTABLE_OPERATORS);
		operatorProducer.save(startDate, endDate, operators);
	}


	private List<StopArea> addZderStopIfNotExists(List<StopArea> stops, StopArea stop) {
		if(stops.stream()
				.noneMatch(stopArea ->
						stopArea.getMappingHastusZdep() != null &&
						stopArea.getMappingHastusZdep().getZder() != null &&
						stop.getMappingHastusZdep() != null &&
						stopArea.getMappingHastusZdep().getZder().equals(stop.getMappingHastusZdep().getZder())))
		{
			stops.add(stop);
		}
		return stops;
	}

	private List<StopArea> addZdlrStopIfNotExists(List<StopArea> stops, StopArea stop) {
		if(stop == null) return stops;
		if(stops.stream()
				.noneMatch(stopArea ->
						stopArea.getMappingHastusZdep() != null &&
						stopArea.getMappingHastusZdep().getZdlr() != null &&
						stop.getMappingHastusZdep() != null &&
						stopArea.getMappingHastusZdep().getZdlr().equals(stop.getMappingHastusZdep().getZdlr())))
		{
			if(stop.getIsExternal().booleanValue()) stops.add(stop);
		}
		return stops;
	}

	private UUID[] getLineUuids(List<MappingLineUUID> mappingLineUUIDList, List<StopPoint> stopPointList, StopArea stop, UUID[] lineIdArray) {
		List<Line> lines = getUsedLines(stopPointList, stop);
		if(lines.size() > 0) {
			List<UUID> uuids = lines.stream()
					.map(line -> mappingLineUUIDList.stream()
							.filter(mappingLineUUID -> mappingLineUUID.getLineId() == line.getId())
							.findFirst()
							.orElse(null)
							.getUuid())
					.distinct()
					.collect(Collectors.toList());
			lineIdArray = new UUID[uuids.size()];
			lineIdArray = uuids.toArray(lineIdArray);
		}
		return lineIdArray;
	}

	private List<Line> getUsedLines(List<StopPoint> stopPointList, StopArea stop) {
		List<Line> lines = stopPointList.stream()
				.filter(sp ->  sp.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId().equals(stop.getObjectId()))
				.map(stopPoint -> stopPoint.getRoute().getLine())
				.distinct()
				.collect(Collectors.toList());
		return lines;
	}

	private StopArea createStopZder(StopArea stop, List<StopArea> stops) {
		return stops.stream()
				.filter(stopArea -> stopArea.getMappingHastusZdep() != null &&
						stopArea.getMappingHastusZdep().getZder() != null &&
						stop.getMappingHastusZdep() != null &&
						stopArea.getMappingHastusZdep().getZder().equals(stop.getMappingHastusZdep().getZder()))
				.findFirst()
				.orElse(getNewStopAreaZdXX(stop));
	}

	private StopArea getNewStopAreaZdXX(StopArea stop) {
		StopArea newStop = new StopArea();
		newStop.setUuid(UUID.randomUUID());
		newStop.setName(stop.getName());
		newStop.setMappingHastusZdep(stop.getMappingHastusZdep());
		newStop.setIsExternal(stop.getIsExternal());
		return newStop;
	}

	private StopArea createStopZdlr(StopArea stop, List<StopArea> stops) {
		if(!stop.getIsExternal().booleanValue()) return null;
		return stops.stream()
				.filter(stopArea -> stopArea.getMappingHastusZdep() != null &&
						stopArea.getMappingHastusZdep().getZder() != null &&
						stop.getMappingHastusZdep() != null &&
						stopArea.getMappingHastusZdep().getZdlr().equals(stop.getMappingHastusZdep().getZdlr()))
				.findFirst()
				.orElse(getNewStopAreaZdXX(stop));
	}


	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new ConcertoSharedDataProducerCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(ConcertoSharedDataProducerCommand.class.getName(), new DefaultCommandFactory());
	}

}
