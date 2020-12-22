package mobi.chouette.exchange.concerto.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import lombok.val;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.JSONUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.concerto.Constant;
import mobi.chouette.exchange.concerto.exporter.producer.AbstractProducer;
import mobi.chouette.exchange.concerto.exporter.producer.ConcertoLineProducer;
import mobi.chouette.exchange.concerto.model.ConcertoLineObjectIdGenerator;
import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Line;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.joda.time.LocalDate;

import javax.naming.InitialContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.UUID;
import java.util.stream.Collectors;

//@todo SCH revoir les trucs de reporters etc après avoir vu le chargement des données
@Log4j
public class ConcertoLineProducerCommand implements Command, Constant {
	public static final String COMMAND = "ConcertoLineProducerCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);
		ConcertoExportParameters parameters = (ConcertoExportParameters) context.get(CONFIGURATION);
		ActionReporter reporter = ActionReporter.Factory.getInstance();
		//@todo sch = voir si on peut pas trouver ces données ailleurs ultérieurement
		List<MappingLineUUID> mappingLineUUIDList = (List<MappingLineUUID>)context.get(MAPPING_LINE_UUID);
		if(mappingLineUUIDList == null){
			mappingLineUUIDList = new ArrayList<>();
		}

		try {

			Line line = (Line) context.get(LINE);

			ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);

			if(collection == null) collection = new ExportableData();

			if(line != null && StringUtils.isEmpty(line.getCodifligne())) {
				log.info("Ignoring line without codifligne : " + ContextHolder.getContext());
				reporter.addObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, "Codifligne manquant",
						OBJECT_STATE.WARNING, IO_TYPE.OUTPUT);
				return SUCCESS;
			}
			else {
				reporter.addObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, NamingUtil.getName(line),
						OBJECT_STATE.OK, IO_TYPE.OUTPUT);
			}

			if (line.getCompany() == null && line.getNetwork() == null) {
				log.info("Ignoring line without company or network: " + line.getObjectId());
				reporter.addErrorToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE,
						ActionReporter.ERROR_CODE.INVALID_FORMAT, "no company for this line");
				return SUCCESS;
			}

			if (line.getCategoriesForLine() == null || !line.getCategoriesForLine().getName().equals("IDFM")) {
				log.info("Ignoring line not idm: " + line.getObjectId());
				reporter.addErrorToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE,
						ActionReporter.ERROR_CODE.INVALID_FORMAT, "not an idfm line");
				return SUCCESS;
			}

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

			ConcertoDataCollector collector = new ConcertoDataCollector();
			boolean cont = collector.collect(collection, line, startDate, endDate);

			for (Timetable t : collection.getTimetables()) {
				if (t.getStartOfPeriod() == null || t.getEndOfPeriod() == null) {
					List<LocalDate> effectiveDates = t.getEffectiveDates();
					if (effectiveDates.size() > 0) {
						Collections.sort(effectiveDates);
						if (t.getStartOfPeriod() == null) {
							t.setStartOfPeriod(effectiveDates.get(0));
						}
						if (t.getEndOfPeriod() == null) {
							t.setEndOfPeriod(effectiveDates.get(effectiveDates.size() - 1));
						}
					} else {
						if (t.getStartOfPeriod() != null) {
							t.setEndOfPeriod(t.getStartOfPeriod());
						} else if (t.getEndOfPeriod() != null) {
							t.setStartOfPeriod(t.getEndOfPeriod());
						} else {
							// Both empty
							t.setStartOfPeriod(org.joda.time.LocalDate.now());
							t.setEndOfPeriod(org.joda.time.LocalDate.now());
						}
					}
				}
			}

			LongSummaryStatistics statsMax = collection.getTimetables().stream()
					.mapToLong(t -> t.getEndOfPeriod().toDate().getTime())
					.summaryStatistics();
			LocalDate maxDate = new LocalDate(statsMax.getMax());

			LongSummaryStatistics statsMin = collection.getTimetables().stream()
					.mapToLong(t -> t.getStartOfPeriod().toDate().getTime())
					.summaryStatistics();
			LocalDate minDate = new LocalDate(statsMin.getMin());

			if(minDate.isAfter(startDate)) startDate = minDate;
			if(maxDate.isBefore(endDate)) endDate = maxDate;


			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 0);
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.ROUTE, collection.getRoutes().size());
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.VEHICLE_JOURNEY,collection.getVehicleJourneys().size());

			if (cont) {
				context.put(EXPORTABLE_DATA, collection);
				UUID uuid = saveLine(context, line, startDate, endDate, parameters);
				reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 1);
				mappingLineUUIDList.add(new MappingLineUUID(uuid, line.getId()));
				result = SUCCESS;
			} else {
				reporter.addErrorToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE,
						ActionReporter.ERROR_CODE.NO_DATA_ON_PERIOD, "no data on period");
				result = SUCCESS; // else export will stop here
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}
		context.put(MAPPING_LINE_UUID, mappingLineUUIDList);
		return result;
	}

	private UUID saveLine(Context context, Line line, LocalDate startDate, LocalDate endDate, ConcertoExportParameters parameters) throws JAXBException, JSONException {
		ConcertoExporter exporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
        ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
		ConcertoLineProducer lineProducer = new ConcertoLineProducer(exporter);

		ConcertoObjectId objectId = ConcertoLineObjectIdGenerator.getConcertoObjectId(line.getCodifligne(), line.getNumber());
		String concertoObjectId = lineProducer.getObjectIdConcerto(objectId, parameters);

		UUID uuid = null;
		boolean hasVj = collection.getVehicleJourneys().stream().anyMatch(vehicleJourney -> vehicleJourney.getRoute().getLine().getId().equals(line.getId()));

		if (hasVj) {
			uuid = lineProducer.save(line, startDate, endDate, concertoObjectId);
		}

		return uuid;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new ConcertoLineProducerCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(ConcertoLineProducerCommand.class.getName(), new DefaultCommandFactory());
	}

}
