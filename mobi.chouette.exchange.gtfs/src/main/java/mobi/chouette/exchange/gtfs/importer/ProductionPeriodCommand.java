package mobi.chouette.exchange.gtfs.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.TimetableDAO;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.gtfs.Constant.GTFS_CALENDAR_FILE;
import static mobi.chouette.exchange.gtfs.Constant.GTFS_REPORTER;


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

        try {

            log.info(Color.LIGHT_MAGENTA + "ProductionPeriodCommand" + Color.NORMAL);

            Referential referential = (Referential) context.get(REFERENTIAL);

            if (referential != null) {

                GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);

                referential.clear(true);

                // On parse l'ensemble des calendriers entrants pour récupérer le start date le plus tôt et la end date la plus tard
                /*GtfsCalendarParser gtfsCalendarParser = (GtfsCalendarParser) ParserFactory
                        .create(GtfsCalendarParser.class.getName());
                gtfsCalendarParser.parse(context);*/

                // On retaille les calendriers déjà présents en base avec le start date récupéré plus haut
                List<Timetable> timetablesDatabase = timetableDAO.findAll();

                // On récupère la end date la plus tardive déjà en base
                Optional<LocalDate> maxEndDateDatabaseOptional = timetablesDatabase.stream()
                        .flatMap(timetable -> timetable.getPeriods().stream())
                        .map(Period::getEndDate)
                        .max(LocalDate::compareTo);

                if (maxEndDateDatabaseOptional.isPresent()) {
                    log.info(Color.LIGHT_MAGENTA + "maxEndDateDatabase: " + maxEndDateDatabaseOptional.get().toString() + Color.NORMAL);
                }

                for (Timetable timetable : timetablesDatabase) {
                    log.info(Color.LIGHT_MAGENTA + "--- TIMETABLE " + timetable.getId() + " ---" + Color.NORMAL);
                    List<Period> newPeriods = new ArrayList<>();

                    List<Period> gtfsPeriods = referential.getTimetables().values().stream()
                            .filter(tt -> timetable.getId() != null && timetable.getId().equals(tt.getId()))
                            .flatMap(tt -> tt.getPeriods().stream())
                            .collect(Collectors.toList());

                    for (Period period : timetable.getPeriods()) {
                        log.info(Color.LIGHT_MAGENTA + "--- PERIOD ---" + Color.NORMAL);

                        for (Period gtfsPeriod : gtfsPeriods) {

                            if (period.getEndDate().isAfter(gtfsPeriod.getStartDate()) && period.getStartDate().isBefore(gtfsPeriod.getStartDate())) {

                                log.info(Color.LIGHT_MAGENTA + "CONDITION 1" + Color.NORMAL);
                                period.setEndDate(gtfsPeriod.getStartDate());
                                newPeriods.add(new Period(gtfsPeriod.getStartDate(), gtfsPeriod.getEndDate()));

                            } else if (period.getEndDate().isBefore(gtfsPeriod.getEndDate()) && period.getStartDate().isEqual(gtfsPeriod.getStartDate())) {

                                log.info(Color.LIGHT_MAGENTA + "CONDITION 2" + Color.NORMAL);
                                period.setEndDate(gtfsPeriod.getEndDate());

                            } else if (period.getStartDate().isAfter(gtfsPeriod.getStartDate())) {

                                log.info(Color.LIGHT_MAGENTA + "--- DB START DATE AFTER GTFS START DATE ---" + Color.NORMAL);
                                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_CALENDAR_FILE, 1, null,
                                        GtfsException.ERROR.DB_START_DATE_AFTER_GTFS_START_DATE, null, null,
                                        GtfsException.ERROR.DB_START_DATE_AFTER_GTFS_START_DATE.toString()), GTFS_CALENDAR_FILE);

                            } else if (period.getEndDate().isAfter(gtfsPeriod.getEndDate())) {

                                log.info(Color.LIGHT_MAGENTA + "--- DB END DATE AFTER GTFS END DATE ---" + Color.NORMAL);
                                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_CALENDAR_FILE, 1, null,
                                        GtfsException.ERROR.DB_END_DATE_AFTER_GTFS_END_DATE, null, null,
                                        GtfsException.ERROR.DB_END_DATE_AFTER_GTFS_END_DATE.toString()), GTFS_CALENDAR_FILE);
                            }
                        }

                    }

                    if (!newPeriods.isEmpty()) {
                        newPeriods.forEach(timetable::addPeriod);
                    }
                }

                /*if (maxEndDateDatabaseOptional.isPresent() && maxEndDateDatabaseOptional.get().isBefore(minStartDateGtfs)) {
                    // Gestion de l'erreur du trou des périodes de production
                    log.info(Color.LIGHT_MAGENTA + "--- MISSING CALENDAR BETWEEN TWO PRODUCTION PERIODS ---" + Color.NORMAL);
                    gtfsValidationReporter.reportError(context, new GtfsException(GTFS_CALENDAR_FILE, 1, null,
                            GtfsException.ERROR.MISSING_CALENDAR_BETWEEN_TWO_PRODUCTION_PERIODS, null, null,
                            GtfsException.ERROR.MISSING_CALENDAR_BETWEEN_TWO_PRODUCTION_PERIODS.toString()), GTFS_CALENDAR_FILE);
                }*/

                timetableDAO.flush();

                // On clear le referential pour ne pas impacter la suite des opérations de parse et mise à jour
                referential.clear(true);

                result = SUCCESS;
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
            ex.printStackTrace();//TODO TEST DEV
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

