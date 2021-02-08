package mobi.chouette.exchange.concerto.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.OperatorDAO;
import mobi.chouette.exchange.concerto.exporter.producer.ConcertoOperatorProducer;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.model.Operator;
import org.joda.time.LocalDate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;

import static mobi.chouette.exchange.concerto.Constant.CONCERTO_EXPORTER;

/**
 *
 */
@Log4j
@Stateless(name = DaoConcertoOperatorProducerCommand.COMMAND)
public class DaoConcertoOperatorProducerCommand implements Command, Constant {

    public static final String COMMAND = "DaoConcertoOperatorProducerCommand";

    @EJB
    private OperatorDAO operatorDAO;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            ConcertoExporter exporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
            ConcertoExportParameters parameters = (ConcertoExportParameters) context.get(CONFIGURATION);
            String objectTypeConcerto = (String) context.get(OBJECT_TYPE_CONCERTO);

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

            List<Operator> operators = operatorDAO.findAll();

            ConcertoOperatorProducer operatorProducer = new ConcertoOperatorProducer(exporter);

            if(!operators.isEmpty()){
                operatorProducer.save(startDate, endDate, operators, objectTypeConcerto);
            }

            result = true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }

        return result;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.concerto/"
                        + COMMAND;
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
        CommandFactory.factories.put(DaoConcertoOperatorProducerCommand.class.getName(),
                new DaoConcertoOperatorProducerCommand.DefaultCommandFactory());
    }

}
