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
import mobi.chouette.exchange.concerto.exporter.producer.ConcertoLineProducer;
import mobi.chouette.exchange.concerto.model.ConcertoLine;
import mobi.chouette.exchange.concerto.model.ConcertoLineObjectIdGenerator;
import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Line;

import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.joda.time.LocalDate;

import javax.naming.InitialContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



/**
 *
 */
@Log4j
public class ConcertoLineCollectCommand implements Command, Constant {
    public static final String COMMAND = "ConcertoLineCollectCommand";

    private final String TYPE = "line";

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);
        ConcertoExportParameters parameters = (ConcertoExportParameters) context.get(CONFIGURATION);
        ActionReporter reporter = ActionReporter.Factory.getInstance();
        //@todo sch = voir si on peut pas trouver ces données ailleurs ultérieurement


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

            ConcertoDataCollector collector = new ConcertoDataCollector();
            boolean cont = collector.collect(collection, line, null, null);

            reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 0);
            reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.ROUTE, collection.getRoutes().size());
            reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.VEHICLE_JOURNEY,collection.getVehicleJourneys().size());

            if (cont) {
                context.put(EXPORTABLE_DATA, collection);
                collectLine(context, line, parameters);
                reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 1);
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
        return result;
    }

    private void collectLine(Context context, Line line, ConcertoExportParameters parameters) throws JAXBException, JSONException {
        List<MappingLineUUID> mappingLineUUIDList = (List<MappingLineUUID>)context.get(MAPPING_LINE_UUID);
        if(mappingLineUUIDList == null){
            mappingLineUUIDList = new ArrayList<>();
        }
        ConcertoExporter exporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
        ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
        String objectTypeConcerto = (String) context.get(OBJECT_TYPE_CONCERTO);
        String provider = (String) context.get(PROVIDER);

        ConcertoLineProducer lineProducer = new ConcertoLineProducer(exporter);
        List<ConcertoLine> concertoLines = new ArrayList<>();


        boolean hasVj = collection.getVehicleJourneys().stream().anyMatch(vehicleJourney -> vehicleJourney.getRoute().getLine().getId().equals(line.getId()));

        if (hasVj && line.getCategoriesForLine().getName().equals("IDFM")) {
            LocalDate startDate = lineProducer.getStartDate(parameters);
            LocalDate endDate = lineProducer.getEndDate(parameters, startDate);

            String[] hastusId = line.getObjectId().split(":");
            ConcertoObjectId objectId = ConcertoLineObjectIdGenerator.getConcertoObjectId(line.getCodifligne(), hastusId[2]);
            String concertoObjectId = lineProducer.getObjectIdConcerto(objectId, objectTypeConcerto);

            UUID uuid = UUID.randomUUID();

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                ConcertoLine concertoLine = save(line, date, concertoObjectId, uuid);
                mappingLineUUIDList.add(new MappingLineUUID(concertoLine.getUuid(), line.getId(), provider));
                concertoLines.add(concertoLine);
            }
        }

        if (context.get(CONCERTO_LINES) != null) {
            concertoLines.addAll((List<ConcertoLine>) context.get(CONCERTO_LINES));
        }
        context.put(CONCERTO_LINES, concertoLines);


        context.put(MAPPING_LINE_UUID, mappingLineUUIDList);

    }


    private ConcertoLine save(Line line, LocalDate date, String objectId, UUID uuid){
        ConcertoLine concertoLine = new ConcertoLine();
        concertoLine.setType(TYPE);
        concertoLine.setUuid(uuid);
        concertoLine.setDate(date);
        concertoLine.setName(line.getNumber());
        concertoLine.setObjectId(objectId);
        concertoLine.setAttributes("{}");
        concertoLine.setReferences("{}");
        concertoLine.setCollectedAlways(true);
        concertoLine.setCollectChildren(true);
        concertoLine.setCollectGeneralMessages(true);
        return concertoLine;
    }


    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new ConcertoLineCollectCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(ConcertoLineCollectCommand.class.getName(), new DefaultCommandFactory());
    }

}

