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
import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import mobi.chouette.exchange.concerto.model.ConcertoStopArea;
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
import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import org.codehaus.jettison.json.JSONException;
import org.joda.time.LocalDate;

import javax.naming.InitialContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.concerto.model.StopAreaTypeEnum.ZDEP;
import static mobi.chouette.exchange.concerto.model.StopAreaTypeEnum.ZDER;
import static mobi.chouette.exchange.concerto.model.StopAreaTypeEnum.ZDLR;

/**
 *
 */
@Log4j
public class ConcertoStopCollectCommand implements Command, Constant {
	public static final String COMMAND = "ConcertoStopCollectCommand";

	private final String TYPE = "stop_area";

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

	private void saveData(Context context) throws JSONException, JAXBException {
		ConcertoExporter exporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
		ConcertoExportParameters parameters = (ConcertoExportParameters) context.get(CONFIGURATION);
		ConcertoStopProducer stopProducer = new ConcertoStopProducer(exporter);
		ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
		List<MappingLineUUID> mappingLineUUIDList = (List<MappingLineUUID>) context.get(MAPPING_LINE_UUID);
		List<StopPoint> stopPointList = collection.getAllParsedStopPoints();
		String objectTypeConcerto = (String) context.get(OBJECT_TYPE_CONCERTO);
		String provider = (String) context.get(PROVIDER);

		LocalDate startDate;
		if (parameters.getStartDate() != null) {
			startDate = LocalDate.fromDateFields(parameters.getStartDate());
		} else {
			startDate = new LocalDate();
		}

		LocalDate endDate;
		if (parameters.getEndDate() != null) {
			endDate = LocalDate.fromDateFields(parameters.getEndDate());
		} else if (context.get(PERIOD) != null) {
			endDate = startDate.plusDays((Integer) context.get(PERIOD) - 1);
		} else {
			endDate = startDate.plusDays(29);
		}

		Set<StopArea> physicalStops = collection.getPhysicalStops();
		List<StopArea> zderStops = new ArrayList<>();

		List<StopArea> zdlrStops = new ArrayList<>();
		List<StopArea> parsedZdlr = new ArrayList<>();
		if(context.containsKey(PARSED_ZDLR) || context.get(PARSED_ZDLR) != null) {
			parsedZdlr = (List<StopArea>) context.get(PARSED_ZDLR);
		}

		MapperLinesAndZone mapperLinesAndZders = new MapperLinesAndZone();
		MapperLinesAndZone mapperLinesAndZdlrs = new MapperLinesAndZone();

		List<ConcertoStopArea> concertoStopAreas = new ArrayList<>();

		// no lambda : esprit chouette
		// zdep
		for (StopArea stop : physicalStops) {
			if (stop.getMappingHastusZdep() == null) continue;
			UUID[] lineIdArray = new UUID[0];
			StopArea stopZder = createStopZder(stop, zderStops);
			lineIdArray = getLineUuids(mappingLineUUIDList, stopPointList, stop, lineIdArray, provider);
			zderStops = addZderStopIfNotExists(zderStops, stopZder);
			mapperLinesAndZders.addMappingZoneLines(stop.getMappingHastusZdep().getZder(), lineIdArray);
			mapperLinesAndZdlrs.addMappingZoneLines(stop.getMappingHastusZdep().getZdlr(), lineIdArray);
			ConcertoObjectId objectId = ConcertoStopAreaZdepObjectIdGenerator.getConcertoObjectId(stop);
			String concertoObjectId = stopProducer.getObjectIdConcerto(objectId, objectTypeConcerto);
			for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
				ConcertoStopArea concertoStopArea =  save(stop, null, stopZder, concertoObjectId, lineIdArray, StopAreaTypeEnum.ZDEP, date);
				concertoStopAreas.add(concertoStopArea);
			}
		}

		//zder
		for (StopArea stop : zderStops) {
			if (stop.getMappingHastusZdep() == null) continue;
			UUID[] lineIdArray = mapperLinesAndZders.getLinesForZone(stop.getMappingHastusZdep().getZder());
			StopArea stopZdlr = createStopZdlr(stop, zdlrStops);
			zdlrStops = addZdlrStopIfNotExists(zdlrStops, stopZdlr);
			ConcertoObjectId objectId = ConcertoStopAreaZderObjectIdGenerator.getConcertoObjectId(stop);
			String concertoObjectId = stopProducer.getObjectIdConcerto(objectId, objectTypeConcerto);
			for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
				ConcertoStopArea concertoStopArea = save(stop, stopZdlr, null, concertoObjectId, lineIdArray, StopAreaTypeEnum.ZDER, date);
				concertoStopAreas.add(concertoStopArea);
			}
		}

		//zdlr
		for (StopArea stop : zdlrStops) {
			boolean wasParsed = parsedZdlr.stream()
					.anyMatch(stopArea -> stopArea.getMappingHastusZdep().getZdlr().equals(stop.getMappingHastusZdep().getZdlr()));
			if (wasParsed) continue;
			if (stop.getMappingHastusZdep() == null) continue;
			if (!stop.getIsExternal().booleanValue()) continue;
			UUID[] lineIdArray = mapperLinesAndZdlrs.getLinesForZone(stop.getMappingHastusZdep().getZdlr());
			ConcertoObjectId objectId = ConcertoStopAreaZdlrObjectIdGenerator.getConcertoObjectId(stop);
			String concertoObjectId = stopProducer.getObjectIdConcerto(objectId, objectTypeConcerto);
			for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
				ConcertoStopArea concertoStopArea = save(stop, null, null, concertoObjectId, lineIdArray, StopAreaTypeEnum.ZDLR, date);
				concertoStopAreas.add(concertoStopArea);
			}
			parsedZdlr.add(stop);
		}
		context.put(PARSED_ZDLR, parsedZdlr);

		if (context.get(CONCERTO_STOP_AREAS) != null) {
			concertoStopAreas.addAll((List<ConcertoStopArea>) context.get(CONCERTO_STOP_AREAS));
		}
		context.put(CONCERTO_STOP_AREAS, concertoStopAreas);

	}


	private List<StopArea> addZderStopIfNotExists(List<StopArea> stops, StopArea stop) {
		if(stop == null) return stops;
		if(stop.getMappingHastusZdep() == null) return stops;
		if(stop.getMappingHastusZdep().getZder() == null) return stops;
		if(stops.stream()
				.noneMatch(stopArea ->
						stopArea.getMappingHastusZdep() != null &&
								stopArea.getMappingHastusZdep().getZder() != null &&
								stopArea.getMappingHastusZdep().getZder().equals(stop.getMappingHastusZdep().getZder())))
		{
			stops.add(stop);
		}
		return stops;
	}

	private List<StopArea> addZdlrStopIfNotExists(List<StopArea> stops, StopArea stop) {
		if(stop == null) return stops;
		if(stop.getIsExternal() == null || !stop.getIsExternal().booleanValue()) return stops;
		if(stop.getMappingHastusZdep() == null) return stops;
		if(stop.getMappingHastusZdep().getZdlr() == null) return stops;
		if(stops.stream()
				.noneMatch(stopArea ->
						stopArea.getMappingHastusZdep() != null &&
								stopArea.getMappingHastusZdep().getZdlr() != null &&
								stopArea.getMappingHastusZdep().getZdlr().equals(stop.getMappingHastusZdep().getZdlr())))
		{
			if(stop.getIsExternal().booleanValue()) stops.add(stop);
		}
		return stops;
	}

	private UUID[] getLineUuids(List<MappingLineUUID> mappingLineUUIDList, List<StopPoint> stopPointList, StopArea stop, UUID[] lineIdArray, String provider) {
		List<Line> lines = getUsedLines(stopPointList, stop);
		if(lines.size() > 0) {
			List<UUID> uuids = lines.stream()
					.map(line -> mappingLineUUIDList.stream()
							.filter(mappingLineUUID -> mappingLineUUID.getLineId().equals(line.getId()) && mappingLineUUID.getProvider().equals(provider))
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
		if(stop == null) return null;
		if(stop.getMappingHastusZdep() == null) return null;
		if(stop.getMappingHastusZdep().getZder() == null) return null;
		return stops.stream()
				.filter(stopArea -> stopArea.getMappingHastusZdep() != null &&
						stopArea.getMappingHastusZdep().getZder() != null &&
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
		if(stop == null) return null;
		if(stop.getMappingHastusZdep() == null ) return null;
		if(stop.getMappingHastusZdep().getZdlr() == null) return null;
		if(stop.getIsExternal() == null) return null;
		if(!stop.getIsExternal().booleanValue()) return null;
		return stops.stream()
				.filter(stopArea -> stopArea.getMappingHastusZdep() != null &&
						stopArea.getMappingHastusZdep().getZder() != null &&
						stopArea.getMappingHastusZdep().getZdlr().equals(stop.getMappingHastusZdep().getZdlr()))
				.findFirst()
				.orElse(getNewStopAreaZdXX(stop));
	}


	private ConcertoStopArea save(StopArea neptuneObject, StopArea parent, StopArea referent, String objectId, UUID[] lines, StopAreaTypeEnum stopAreaType, LocalDate date)
	{
		UUID uuid;
		ConcertoStopArea concertoStopArea;

		if(neptuneObject.getUuid() != null) {
			uuid = neptuneObject.getUuid();
		} else {
			uuid = UUID.randomUUID();
		}

		concertoStopArea = save(neptuneObject, parent, referent, date, uuid, objectId, lines, stopAreaType);

		return concertoStopArea;
	}

	private ConcertoStopArea save(StopArea neptuneObject, StopArea parent, StopArea referent, LocalDate date, UUID uuid, String objectId, UUID[] lines, StopAreaTypeEnum stopAreaType) {
		ConcertoStopArea stop = new ConcertoStopArea();

		switch (stopAreaType) {
			case ZDEP:
				break;
			case ZDER:
				if (neptuneObject.getMappingHastusZdep() == null || neptuneObject.getMappingHastusZdep().getZder() == null)
					return null;
				break;
			case ZDLR:
				if (neptuneObject.getMappingHastusZdep() == null || neptuneObject.getMappingHastusZdep().getZdlr() == null)
					return null;
				break;
			default:
				return null;
		}

		stop.setType(TYPE);
		stop.setUuid(uuid);
		if (stopAreaType == ZDEP && referent != null) {
			stop.setReferent_uuid(referent.getUuid());
		} else {
			stop.setReferent_uuid(null);
		}

		if (stopAreaType == ZDER && parent != null) {
			stop.setParent_uuid(parent.getUuid());
		} else {
			stop.setParent_uuid(null);
		}

		// If name is empty, try to use parent name
		String name = neptuneObject.getName();
		if (stopAreaType == ZDLR && neptuneObject.getParent() != null) {
			name = neptuneObject.getParent().getName();
		}
		if (name == null) {
			return null;
		}
		stop.setDate(date);
		stop.setName(name);
		stop.setObjectId(objectId);

		MappingHastusZdep mappingHastusZdep = neptuneObject.getMappingHastusZdep();
		// @todo SCH PA NON IDFM ? A check avec Vincent
		if (mappingHastusZdep == null) return null;
		stop.setZdep(mappingHastusZdep.getZdep());
		stop.setZder(mappingHastusZdep.getZder());
		stop.setZdlr(mappingHastusZdep.getZdlr());
		stop.setAttributes("{}");
		stop.setReferences("{}");
		stop.setCollectedAlways(true);
		if (stopAreaType == ZDLR) {
			stop.setLines(new UUID[0]);
			stop.setCollectChildren(true);
		} else {
			stop.setLines(lines);
			stop.setCollectChildren(false);
		}
		stop.setCollectGeneralMessages(true);

		return stop;
	}



	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new ConcertoStopCollectCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(ConcertoStopCollectCommand.class.getName(), new DefaultCommandFactory());
	}

}
