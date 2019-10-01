package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ZdepPlageDAO;
import mobi.chouette.model.ZdepPlage;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;


@Log4j
@Stateless(name = ZdepPlageCommand.COMMAND)
public class ZdepPlageCommand implements Command {

	public static final String COMMAND = "ZdepPlageCommand";

	@EJB
	private ZdepPlageDAO zdepPlageDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean execute(Context context) throws Exception {

		boolean result = ERROR;
		final Map<String, InputStream> inputStreamsByName = (Map<String, InputStream>) context.get("inputStreamByName");

		String inputStreamName = selectDataInputStreamName(inputStreamsByName);
		if (inputStreamName != null) {
			CSVParser csvParser = new CSVParser(new InputStreamReader(inputStreamsByName.get(inputStreamName)), CSVFormat.DEFAULT);
			int headerNbLine = 1;
			int cpt = 0;
			for (CSVRecord csvRecord : csvParser) {
				if(cpt < headerNbLine) {
					cpt++;
					continue;
				}
				if(csvRecord.size() > 1) {
					throw new Exception("Format de fichier invalide");
				}

				String zdep = csvRecord.get(0);
				if(zdep == null || zdep.length() == 0) continue;

				ZdepPlage byZdep = zdepPlageDAO.findByZdep(zdep);
				if(byZdep == null) {
					ZdepPlage zdepPlage = new ZdepPlage();
					zdepPlage.setZdep(zdep);
					zdepPlage.setConsumed(false);
					zdepPlageDAO.create(zdepPlage);
				}
			}
			result = SUCCESS;
		}

		return result;
	}

	private String selectDataInputStreamName(final Map<String, InputStream> inputStreamsByName) {
		for (String name : inputStreamsByName.keySet()) {
			if (!name.equals(PARAMETERS_FILE)) {
				return name;
			}
		}
		return null;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange/" + COMMAND;
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
		CommandFactory.factories.put(ZdepPlageCommand.class.getName(), new DefaultCommandFactory());
	}
}
