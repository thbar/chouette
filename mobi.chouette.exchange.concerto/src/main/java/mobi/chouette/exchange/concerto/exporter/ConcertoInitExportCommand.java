package mobi.chouette.exchange.concerto.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.concerto.Constant;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;
import mobi.chouette.exchange.metadata.Metadata;
import mobi.chouette.model.util.Referential;
import org.joda.time.LocalDateTime;

import javax.naming.InitialContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Log4j
public class ConcertoInitExportCommand implements Command, Constant {

	public static final String COMMAND = "ConcertoInitExportCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;

		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			JobData jobData = (JobData) context.get(JOB_DATA);
			jobData.setOutputFilename("conversion_" + jobData.getType() + "_" + jobData.getId() + ".csv");
			context.put(REFERENTIAL, new Referential());
			
			Metadata metadata = new Metadata(); // if not asked, will be used as dummy
			metadata.setDate(LocalDateTime.now());
			metadata.setFormat("text/csv");
			metadata.setTitle("Export Concerto ");

			context.put(METADATA, metadata);
			// prepare exporter
			Path path = Paths.get(jobData.getPathName(), OUTPUT);
			if (!Files.exists(path)) {
				Files.createDirectories(path);
			}
			ConcertoExporter concertoExporter = new ConcertoExporter(path.toString());
			context.put(CONCERTO_EXPORTER, concertoExporter);
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
			Command result = new ConcertoInitExportCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(ConcertoInitExportCommand.class.getName(), new DefaultCommandFactory());
	}

}
