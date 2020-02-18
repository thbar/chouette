package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.parser.GtfsAgencyParser;
import mobi.chouette.exchange.gtfs.parser.GtfsCalendarParser;
import mobi.chouette.exchange.gtfs.parser.GtfsRouteParser;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.Period;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;

import javax.ejb.Stateless;
import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.gtfs.Constant.GTFS_CALENDAR_FILE;
import static mobi.chouette.exchange.gtfs.Constant.GTFS_REPORTER;

@Log4j
@Stateless(name = GtfsRouteParserCommand.COMMAND)
public class GtfsRouteParserCommand implements Command, Constant {

    public static final String COMMAND = "GtfsRouteParserCommand";

    @Getter
    @Setter
    private String gtfsRouteId;

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            Referential referential = (Referential) context.get(REFERENTIAL);
            if (referential != null) {
                referential.clear(true);
            }

            GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

            ActionReporter reporter = ActionReporter.Factory.getInstance();

            // PTNetwork
            if (referential.getSharedPTNetworks().isEmpty()) {
                createPTNetwork(referential, configuration);
                reporter.addObjectReport(context, "merged", OBJECT_TYPE.NETWORK, "networks", OBJECT_STATE.OK,
                        IO_TYPE.INPUT);
                reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.NETWORK, OBJECT_TYPE.NETWORK,
                        referential.getSharedPTNetworks().size());
            }

            // Company
            if (referential.getSharedCompanies().isEmpty()) {
                GtfsAgencyParser gtfsAgencyParser = (GtfsAgencyParser) ParserFactory.create(GtfsAgencyParser.class
                        .getName());
                gtfsAgencyParser.parse(context);
                reporter.addObjectReport(context, "merged", OBJECT_TYPE.COMPANY, "companies", OBJECT_STATE.OK,
                        IO_TYPE.INPUT);
                reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.COMPANY, OBJECT_TYPE.COMPANY,
                        referential.getSharedCompanies().size());
            }

//			// StopArea
//			if (referential.getSharedStopAreas().isEmpty()) {
//				GtfsStopParser gtfsStopParser = (GtfsStopParser) ParserFactory.create(GtfsStopParser.class.getName());
//				gtfsStopParser.parse(context);
//			}
//
//			// ConnectionLink
//			if (importer.hasTransferImporter()) {
//				if (referential.getSharedConnectionLinks().isEmpty()) {
//					GtfsTransferParser gtfsTransferParser = (GtfsTransferParser) ParserFactory
//							.create(GtfsTransferParser.class.getName());
//					gtfsTransferParser.parse(context);
//				}
//			}
//
//			if (configuration.getMaxDistanceForCommercial() > 0)
//			{
//				CommercialStopGenerator commercialStopGenerator = new CommercialStopGenerator();
//				commercialStopGenerator.createCommercialStopPoints(context);
//				configuration.setMaxDistanceForCommercial(0);
//			}
//			
//			if (configuration.getMaxDistanceForConnectionLink() > 0)
//			{
//			    ConnectionLinkGenerator connectionLinkGenerator = new ConnectionLinkGenerator();
//				connectionLinkGenerator.createConnectionLinks(context);
//				configuration.setMaxDistanceForConnectionLink(0);
//			}

            // Timetable
            if (referential.getSharedTimetables().isEmpty()) {
                GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
                GtfsCalendarParser gtfsCalendarParser = (GtfsCalendarParser) ParserFactory
                        .create(GtfsCalendarParser.class.getName());
                gtfsCalendarParser.parse(context);
                reporter.addObjectReport(context, "merged", OBJECT_TYPE.TIMETABLE, "time tables", OBJECT_STATE.OK,
                        IO_TYPE.INPUT);
                reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.TIMETABLE, OBJECT_TYPE.TIMETABLE,
                        referential.getSharedTimetables().size());

                List<Timetable> dbTimetables = (List<Timetable>) context.get(DB_TIMETABLES);

                for (Timetable gtfsTimetable : referential.getTimetables().values()) {

                    List<Period> newPeriods = new ArrayList<>();
                    List<Period> dbPeriods = dbTimetables.stream()
                            .filter(tt -> StringUtils.equals(tt.getObjectId(), gtfsTimetable.getObjectId()))
                            .flatMap(tt -> {
                                if (tt.getPeriods() != null) {
                                    return tt.getPeriods().stream();
                                } else {
                                    return new ArrayList<Period>().stream();
                                }
                            })
                            .collect(Collectors.toList());

                    if (!dbPeriods.isEmpty()) {

                        // Récupère la période la plus ancienne du timetable courant
                        Period minDbPeriod = null;
                        for (Period dbPeriod : dbPeriods) {

                            if (minDbPeriod == null) {
                                minDbPeriod = dbPeriod;
                            } else if (dbPeriod.getStartDate().isAfter(minDbPeriod.getStartDate())) {
                                minDbPeriod = dbPeriod;
                            }
                        }

                        // Récupère la période la plus récente du timetable courant
                        Period maxDbPeriod = null;
                        for (Period dbPeriod : dbPeriods) {

                            if (maxDbPeriod == null) {
                                maxDbPeriod = dbPeriod;
                            } else if (dbPeriod.getEndDate().isAfter(maxDbPeriod.getEndDate())) {
                                maxDbPeriod = dbPeriod;
                            }
                        }

                        if (maxDbPeriod != null) {

                            for (Period gtfsPeriod : gtfsTimetable.getPeriods()) {

                                // BDD : |----------|
                                // GTFS:    |----|
                                if (maxDbPeriod.getStartDate().isBefore(gtfsPeriod.getStartDate()) && maxDbPeriod.getEndDate().isAfter(gtfsPeriod.getEndDate())) {

                                    LocalDate gtfsStartDate = gtfsPeriod.getStartDate();
                                    LocalDate gtfsEndDate = gtfsPeriod.getEndDate();
                                    gtfsPeriod.setStartDate(maxDbPeriod.getStartDate());
                                    gtfsPeriod.setEndDate(gtfsStartDate.minusDays(1));
                                    newPeriods.add(new Period(gtfsStartDate, gtfsEndDate.minusDays(1)));
                                    newPeriods.add(new Period(gtfsEndDate, maxDbPeriod.getEndDate()));

                                }
                                // BDD : |----------|
                                // GTFS:       |----------|
                                else if (maxDbPeriod.getEndDate().isAfter(gtfsPeriod.getStartDate()) && maxDbPeriod.getStartDate().isBefore(gtfsPeriod.getStartDate())) {

                                    LocalDate gtfsStartDate = gtfsPeriod.getStartDate();
                                    newPeriods.add(new Period(gtfsStartDate, gtfsPeriod.getEndDate()));
                                    gtfsPeriod.setStartDate(maxDbPeriod.getStartDate());
                                    gtfsPeriod.setEndDate(gtfsStartDate.minusDays(1));

                                }
								// BDD :    |-------|
								// GTFS: |----------|
                                else if (maxDbPeriod.getStartDate().isAfter(gtfsPeriod.getStartDate())) {

                                    gtfsValidationReporter.reportError(context, new GtfsException(GTFS_CALENDAR_FILE, 1, null,
                                            GtfsException.ERROR.DB_START_DATE_AFTER_GTFS_START_DATE, null, null,
                                            GtfsException.ERROR.DB_START_DATE_AFTER_GTFS_START_DATE.toString()), GTFS_CALENDAR_FILE);

                                }
								// BDD : |----------|
								// GTFS: |-------|
                                else if (maxDbPeriod.getEndDate().isAfter(gtfsPeriod.getEndDate())) {

                                    gtfsValidationReporter.reportError(context, new GtfsException(GTFS_CALENDAR_FILE, 1, null,
                                            GtfsException.ERROR.DB_END_DATE_AFTER_GTFS_END_DATE, null, null,
                                            GtfsException.ERROR.DB_END_DATE_AFTER_GTFS_END_DATE.toString()), GTFS_CALENDAR_FILE);

                                } else if (!(maxDbPeriod.getEndDate().isBefore(gtfsPeriod.getEndDate()) && maxDbPeriod.getStartDate().isEqual(gtfsPeriod.getStartDate()))) {

                                    //Vérifie la présence de trous dans les périodes de productions
                                    if (minDbPeriod.getStartDate().minusDays(1).isAfter(gtfsPeriod.getEndDate()) || maxDbPeriod.getEndDate().plusDays(1).isBefore(gtfsPeriod.getStartDate())) {
                                        gtfsValidationReporter.reportError(context, new GtfsException(GTFS_CALENDAR_FILE, 1, null,
                                                GtfsException.ERROR.MISSING_CALENDAR_BETWEEN_TWO_PRODUCTION_PERIODS, null, null,
                                                GtfsException.ERROR.MISSING_CALENDAR_BETWEEN_TWO_PRODUCTION_PERIODS.toString()), GTFS_CALENDAR_FILE);
                                    }

                                    gtfsPeriod.setStartDate(maxDbPeriod.getStartDate());
                                    gtfsPeriod.setEndDate(maxDbPeriod.getEndDate());
                                }
                            }

                            // Préserve les périodes précédentes de la BDD
                            for (Period dbPeriod : dbPeriods) {

                                // Condition permettant d'ignorer la période la plus récente car déjà traitée ci-dessus
                                if (!dbPeriod.getStartDate().isEqual(maxDbPeriod.getStartDate()) && !dbPeriod.getEndDate().isEqual(maxDbPeriod.getEndDate())) {
                                    newPeriods.add(new Period(dbPeriod.getStartDate(), dbPeriod.getEndDate()));
                                }
                            }
                        }
                    }

                    if (!newPeriods.isEmpty()) {
                        gtfsTimetable.getPeriods().addAll(newPeriods);
                    }
                }
            }

            // Line
            GtfsRouteParser gtfsRouteParser = (GtfsRouteParser) ParserFactory.create(GtfsRouteParser.class.getName());
            gtfsRouteParser.setGtfsRouteId(gtfsRouteId);
            gtfsRouteParser.parse(context);

            addStats(context, referential);
            result = SUCCESS;
        } catch (Exception e) {
            log.error("error : ", e);
            throw e;
        }

        log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        return result;
    }

    private Network createPTNetwork(Referential referential, GtfsImportParameters configuration) {
        String prefix = configuration.getObjectIdPrefix();
        String ptNetworkId = prefix + ":" + Network.PTNETWORK_KEY + ":" + prefix;
        Network ptNetwork = ObjectFactory.getPTNetwork(referential, ptNetworkId);
        ptNetwork.setVersionDate(LocalDate.now());
        ptNetwork.setName(prefix);
        ptNetwork.setRegistrationNumber(prefix);
        ptNetwork.setSourceName("GTFS");
        return ptNetwork;
    }

    private void addStats(Context context, Referential referential) {
        ActionReporter reporter = ActionReporter.Factory.getInstance();

        Line line = referential.getLines().values().iterator().next();
        reporter.addObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, NamingUtil.getName(line),
                OBJECT_STATE.OK, IO_TYPE.INPUT);
        reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 1);
        reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.JOURNEY_PATTERN,
                referential.getJourneyPatterns().size());
        reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.ROUTE, referential
                .getRoutes().size());
        reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.VEHICLE_JOURNEY,
                referential.getVehicleJourneys().size());
        reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.INTERCHANGE,
                referential.getInterchanges().size());

    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new GtfsRouteParserCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(GtfsRouteParserCommand.class.getName(), new DefaultCommandFactory());
    }
}
