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
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import java.io.IOException;
import java.util.List;

//@todo SCH revoir les trucs de reporters etc après avoir vu le chargement des données
@Log4j
public class ConcertoLineProducerCommand implements Command, Constant {
	public static final String COMMAND = "ConcertoLineProducerCommand";

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean execute(Context context) {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			List<ConcertoLine> concertoLines = (List<ConcertoLine>) context.get(CONCERTO_LINES);
			if (concertoLines == null) {
				return ERROR;
			}

			ConcertoExporter exporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
			ConcertoLineProducer lineProducer = new ConcertoLineProducer(exporter);

			lineProducer.save(context, concertoLines);

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
			Command result = new ConcertoLineProducerCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(ConcertoLineProducerCommand.class.getName(), new DefaultCommandFactory());
	}

}
