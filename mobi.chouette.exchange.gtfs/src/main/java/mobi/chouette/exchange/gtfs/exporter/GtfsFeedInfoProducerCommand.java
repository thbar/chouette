/**
 * Projet CHOUETTE
 * <p>
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 */

package mobi.chouette.exchange.gtfs.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsFeedInfoProducer;
import mobi.chouette.exchange.gtfs.model.GtfsFeedInfo;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporter;
import mobi.chouette.model.FeedInfo;

import javax.naming.InitialContext;
import java.io.IOException;

/**
 *
 */
@Log4j
public class GtfsFeedInfoProducerCommand implements Command, Constant {
    public static final String COMMAND = "GtfsFeedInfoProducerCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            GtfsExporter exporter = (GtfsExporter) context.get(GTFS_EXPORTER);
            GtfsFeedInfoProducer feedInfoProducer = new GtfsFeedInfoProducer(exporter);
            FeedInfo feedInfo = (FeedInfo) context.get(FEED_INFO);
            feedInfoProducer.save(new GtfsFeedInfo(feedInfo));
            return SUCCESS;
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
            Command result = new GtfsFeedInfoProducerCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(GtfsFeedInfoProducerCommand.class.getName(), new DefaultCommandFactory());
    }

}
