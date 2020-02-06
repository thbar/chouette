package mobi.chouette.exchange.gtfs.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.TimetableDAO;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.gtfs.parser.GtfsCalendarParser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.updater.TimetableUpdater;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Period;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.util.Referential;
import org.joda.time.LocalDate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Log4j
@Stateless(name = ProductionPeriodCommand.COMMAND)
public class ProductionPeriodCommand implements Command, Constant {

    public static final String COMMAND = "ProductionPeriodCommand";

    @EJB
    private TimetableDAO timetableDAO;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        Referential referential = (Referential) context.get(REFERENTIAL);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        Optional<LocalDate> minStartDate;
        Optional<LocalDate> maxEndDate;
        Optional<LocalDate> maxEndDateDatabase;

        try {

            if (referential != null) {
                referential.clear(true);
            }

            // On parse l'ensemble des calendriers entrants et sortants pour récupérer le start date le plus tôt et la end date la plus tard
            GtfsCalendarParser gtfsCalendarParser = (GtfsCalendarParser) ParserFactory
                    .create(GtfsCalendarParser.class.getName());
            gtfsCalendarParser.parse(context);

            if (referential != null) {
                minStartDate = referential.getTimetables().values().stream()
                        .flatMap(timetable -> timetable.getPeriods().stream())
                        .map(Period::getStartDate)
                        .min(LocalDate::compareTo);

                maxEndDate = referential.getTimetables().values().stream()
                        .flatMap(timetable -> timetable.getPeriods().stream())
                        .map(Period::getEndDate)
                        .min(LocalDate::compareTo);
            }

            // On retaille les calendriers déjà présents en base avec le start date récupéré plus haut
            List<Timetable> timetableList = timetableDAO.findAll();

            // On récupère la end date la plus tardive déjà en base
            maxEndDateDatabase = timetableList.stream()
                    .flatMap(timetable -> timetable.getPeriods().stream())
                    .map(Period::getEndDate)
                    .max(LocalDate::compareTo);


            for (Timetable oldTimetable : timetableList) {
                if (minStartDate.isPresent()){
                    LocalDate finalMinStartDate = minStartDate != null ? minStartDate.minusDays(1) : null;
                }
                if (finalMinStartDate != null) {
                    oldTimetable.getPeriods().forEach(period -> {
                        if (period.getEndDate().isAfter(finalMinStartDate.plusDays(1))) {
                            period.setEndDate(finalMinStartDate);
                        } else if (period.getEndDate().isBefore(finalMinStartDate)) {
                            // Gestion de l'erreur du trou des périodes de production
                            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_CALENDAR_FILE, 1, null,
                                    GtfsException.ERROR.MISSING_CALENDAR_BETWEEN_TWO_PRODUCTION_PERIODS, null, null), GTFS_CALENDAR_FILE);
                        }
                    });
                }
            }
            timetableDAO.flush();

            // On clear le referential pour ne pas impacter la suite des opérations de parse et mise à jour
            if (referential != null) {
                referential.clear(true);
            }

            result = SUCCESS;
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        return result;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.gtfs/" + COMMAND;
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
        CommandFactory.factories.put(ProductionPeriodCommand.class.getName(), new DefaultCommandFactory());
    }
}

