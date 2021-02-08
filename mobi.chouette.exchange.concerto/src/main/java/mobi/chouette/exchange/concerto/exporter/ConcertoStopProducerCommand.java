package mobi.chouette.exchange.concerto.exporter;

/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.concerto.Constant;
import mobi.chouette.exchange.concerto.exporter.producer.ConcertoStopProducer;
import mobi.chouette.exchange.concerto.model.ConcertoStopArea;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;
import mobi.chouette.exchange.report.ActionReporter;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.List;

/**
 *
 */
@Log4j
public class ConcertoStopProducerCommand implements Command, Constant  {
    public static final String COMMAND = "ConcertoStopProducerCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);
        ActionReporter reporter = ActionReporter.Factory.getInstance();

        try {

            List<ConcertoStopArea> concertoStopAreas = (List<ConcertoStopArea>) context.get(CONCERTO_STOP_AREAS);
            if (concertoStopAreas == null) {
                return ERROR;
            }

            ConcertoExporter exporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
            ConcertoStopProducer stopProducer = new ConcertoStopProducer(exporter);

            stopProducer.save(concertoStopAreas);

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
            Command result = new ConcertoStopProducerCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(ConcertoStopProducerCommand.class.getName(), new DefaultCommandFactory());
    }
}
