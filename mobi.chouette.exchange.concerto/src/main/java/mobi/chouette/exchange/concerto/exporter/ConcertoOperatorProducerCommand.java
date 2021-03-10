package mobi.chouette.exchange.concerto.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.concerto.exporter.producer.ConcertoOperatorProducer;
import mobi.chouette.exchange.concerto.model.ConcertoOperator;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;
import mobi.chouette.exchange.gtfs.Constant;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import java.io.IOException;
import java.util.List;

import static mobi.chouette.exchange.concerto.Constant.CONCERTO_EXPORTER;
import static mobi.chouette.exchange.concerto.Constant.CONCERTO_OPERATORS;

/**
 *
 */
@Log4j
public class ConcertoOperatorProducerCommand implements Command, Constant {

    public static final String COMMAND = "ConcertoOperatorProducerCommand";

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            List<ConcertoOperator> concertoOperators = (List<ConcertoOperator>) context.get(CONCERTO_OPERATORS);
            if (concertoOperators == null) {
                return ERROR;
            }

            ConcertoExporter exporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
            ConcertoOperatorProducer operatorProducer = new ConcertoOperatorProducer(exporter);

            operatorProducer.save(concertoOperators);

            result = SUCCESS;
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
            Command result = new ConcertoOperatorProducerCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(ConcertoOperatorProducerCommand.class.getName(), new ConcertoOperatorProducerCommand.DefaultCommandFactory());
    }

}
