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
import mobi.chouette.exchange.concerto.exporter.producer.ConcertoStopProducer;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Line;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import org.joda.time.LocalDate;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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


		for(Timetable t : collection.getTimetables().stream().collect(Collectors.toList())) {
			if(t.getStartOfPeriod() == null || t.getEndOfPeriod() == null) {
				List<org.joda.time.LocalDate> effectiveDates = t.getEffectiveDates();
				if(effectiveDates.size() > 0) {
					Collections.sort(effectiveDates);
					if(t.getStartOfPeriod() == null) {
						t.setStartOfPeriod(effectiveDates.get(0));
					}
					if(t.getEndOfPeriod() == null) {
						t.setEndOfPeriod(effectiveDates.get(effectiveDates.size()-1));
					}
				} else {
					if(t.getStartOfPeriod() != null && t.getEndOfPeriod() == null) {
						t.setEndOfPeriod(t.getStartOfPeriod());
					} else if(t.getEndOfPeriod() != null && t.getStartOfPeriod() == null) {
						t.setStartOfPeriod(t.getEndOfPeriod());
					} else {
						// Both empty
						t.setStartOfPeriod(org.joda.time.LocalDate.now());
						t.setEndOfPeriod(org.joda.time.LocalDate.now());
					}
				}
			}
		}

		LocalDate maxDate = null;
		for(Timetable t : collection.getTimetables().stream().collect(Collectors.toList())) {
			if(maxDate == null){
				maxDate = t.getEndOfPeriod();
			} else if(maxDate.isBefore(t.getEndOfPeriod())){
				maxDate = t.getEndOfPeriod();
			}
		}

		LocalDate minDate = null;
		for(Timetable t : collection.getTimetables().stream().collect(Collectors.toList())) {
			if(minDate == null){
				minDate = t.getStartOfPeriod();
			} else if(minDate.isAfter(t.getStartOfPeriod())){
				minDate = t.getStartOfPeriod();
			}
		}

		if(minDate.isAfter(startDate)) startDate = minDate;
		if(maxDate.isBefore(endDate)) endDate = maxDate;

		Set<StopArea> physicalStops = collection.getPhysicalStops();
		List<StopArea> zderStops = new ArrayList<>();

		List<StopArea> zdlrStops = new ArrayList<>();
		List<StopArea> parsedZdlr = new ArrayList<>();
		if(context.containsKey(PARSED_ZDLR) || context.get(PARSED_ZDLR) != null) {
			parsedZdlr = (List<StopArea>) context.get(PARSED_ZDLR);
		}

		MapperLinesAndZone mapperLinesAndZders = new MapperLinesAndZone();
		MapperLinesAndZone mapperLinesAndZdlrs = new MapperLinesAndZone();


		context.put(PARSED_ZDLR, parsedZdlr);


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


	private StopArea getNewStopAreaZdXX(StopArea stop) {
		StopArea newStop = new StopArea();
		newStop.setUuid(UUID.randomUUID());
		newStop.setName(stop.getName());
		newStop.setIsExternal(stop.getIsExternal());
		return newStop;
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
