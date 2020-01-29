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
import java.time.LocalDate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;


@Log4j
@Stateless(name = ProductionPeriodCommand.COMMAND)
public class ProductionPeriodCommand implements Command, Constant {

    public static final String COMMAND = "ProductionPeriodCommand";

    @EJB
    private TimetableDAO timetableDAO;

    private TimetableUpdater timetableUpdater;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        Referential referential = (Referential) context.get(REFERENTIAL);
        LocalDate minStartDate = null;

        try {

            if (referential != null) {
                referential.clear(true);
            }

            // On parse l'ensemble des calendriers pour récupérer le start date le plus tôt
            GtfsCalendarParser gtfsCalendarParser = (GtfsCalendarParser) ParserFactory
                    .create(GtfsCalendarParser.class.getName());
            gtfsCalendarParser.parse(context);

            if (referential != null) {
                for (Timetable timetable : referential.getTimetables().values()) {
                    for (Period period : timetable.getPeriods()) {
                        if (minStartDate == null) {
                            minStartDate = period.getStartDate();
                        } else {
                            if (period.getStartDate().compareTo(minStartDate) < 0) {
                                minStartDate = period.getStartDate();
                            }
                        }
                    }
                }
            }

            // On retaille les calendriers déjà présents en base avec le start date récupéré plus haut
            List<Timetable> timetableList = timetableDAO.findAll();

            for (Timetable oldTimetable : timetableList) {
                LocalDate finalMinStartDate = minStartDate != null ? minStartDate.minusDays(1) : null;
                oldTimetable.getPeriods().forEach(period -> {
                    if (period.getEndDate().compareTo(finalMinStartDate) >= 0)
                        period.setEndDate(finalMinStartDate);
                });
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

