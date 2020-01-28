package mobi.chouette.exchange.concerto.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.concerto.Constant;
import mobi.chouette.exchange.concerto.exporter.producer.ConcertoLineProducer;
import mobi.chouette.exchange.concerto.model.ConcertoLineObjectIdGenerator;
import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Line;
import mobi.chouette.model.util.NamingUtil;
import org.joda.time.LocalDate;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

			reporter.addObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, NamingUtil.getName(line),
					OBJECT_STATE.OK, IO_TYPE.OUTPUT);
			if (line.getCompany() == null && line.getNetwork() == null) {
				log.info("Ignoring line without company or network: " + line.getObjectId());
				reporter.addErrorToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE,
						ActionReporter.ERROR_CODE.INVALID_FORMAT, "no company for this line");
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
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 0);
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.ROUTE, collection.getRoutes().size());
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.VEHICLE_JOURNEY,collection.getVehicleJourneys().size());

			if (cont) {
				context.put(EXPORTABLE_DATA, collection);
				ConcertoObjectId objectId = ConcertoLineObjectIdGenerator.getConcertoObjectId(line.getCodifligne(), line.getNumber());
				UUID uuid = saveLine(context, line, startDate, endDate, objectId);
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

	private UUID saveLine(Context context, Line line, LocalDate startDate, LocalDate endDate, ConcertoObjectId objectId) {
		ConcertoExporter exporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
        ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
		ConcertoLineProducer lineProducer = new ConcertoLineProducer(exporter);

		UUID uuid = null;
		boolean hasVj = collection.getVehicleJourneys().stream().anyMatch(vehicleJourney -> vehicleJourney.getRoute().getLine().getId() == line.getId());

		if (hasVj) {
			uuid = lineProducer.save(line, startDate, endDate, objectId);
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
