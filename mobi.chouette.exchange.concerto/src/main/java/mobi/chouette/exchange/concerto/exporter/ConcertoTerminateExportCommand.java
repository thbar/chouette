package mobi.chouette.exchange.concerto.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.concerto.Constant;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;

import javax.naming.InitialContext;
import java.io.IOException;

@Log4j
public class ConcertoTerminateExportCommand implements Command, Constant {

	public static final String COMMAND = "ConcertoTerminateExportCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;

		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			ConcertoExporter gtfsExporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
			gtfsExporter.dispose(context);
			result = SUCCESS;

		} catch (Exception e) {
			log.error(e, e);
			throw e;
		} finally {
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new ConcertoTerminateExportCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(ConcertoTerminateExportCommand.class.getName(), new DefaultCommandFactory());
	}

}
