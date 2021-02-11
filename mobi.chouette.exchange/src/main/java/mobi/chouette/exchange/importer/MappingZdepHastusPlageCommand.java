package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.MappingHastusZdepDAO;
import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.persistence.hibernate.ContextHolder;
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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;


@Log4j
@Stateless(name = MappingZdepHastusPlageCommand.COMMAND)
public class MappingZdepHastusPlageCommand implements Command {

	public static final String COMMAND = "MappingZdepHastusPlageCommand";

	@EJB
	private MappingHastusZdepDAO mappingHastusZdepDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean execute(Context context) throws Exception {

		boolean result = ERROR;
		final Map<String, InputStream> inputStreamsByName = (Map<String, InputStream>) context.get("inputStreamByName");

		String inputStreamName = selectDataInputStreamName(inputStreamsByName);
		String hastusOriginal = null;
		String zdep;

		if (inputStreamName != null) {
			CSVParser csvParser = new CSVParser(new InputStreamReader(inputStreamsByName.get(inputStreamName), StandardCharsets.UTF_8), CSVFormat.DEFAULT);
			int headerNbLine = 1;
			int cpt = 0;
			for (CSVRecord csvRecord : csvParser) {
				// entête
				if(cpt < headerNbLine) {
					cpt++;
					if(validateHeader(csvRecord)) {
						continue;
					} else {
						return ERROR;
					}
				}

				if(csvRecord.size() == 0) {
					throw new Exception("Format de fichier invalide");
				}

				zdep = csvRecord.get(0);
				if(zdep == null || zdep.length() == 0) continue;

				if(csvRecord.size() > 1) hastusOriginal = csvRecord.get(1);

				Optional<MappingHastusZdep> daoByZdep = mappingHastusZdepDAO.findByZdep(zdep);
				if (daoByZdep.isPresent()) {
					log.warn("Numéro ZDEP " + zdep + " déjà existant");
					continue;
				}

				createMapping(zdep, hastusOriginal);
			}
		}

		// update zdep des nouveaux lignes si besoin
		Command command = null;
		try {
			command = CommandFactory.create(new InitialContext(), UpdateAllLinesZdepInfosCommand.class.getName());
			command.execute(context);
		} catch (Exception e) {
			log.error("MAJ des zdep KO depuis l'import");
		}
		result = SUCCESS;
		return result;
	}

	private boolean validateHeader(CSVRecord csvRecord) {
		if(!csvRecord.get((0)).toLowerCase().equals("idfcod")){
			return ERROR;
		}
		if(csvRecord.size() == 1) return SUCCESS;
		if(!csvRecord.get((1)).toLowerCase().equals("idfarr")){
			return ERROR;
		}
		return SUCCESS;
	}

	private MappingHastusZdep createMapping(String zdep, String hastusoriginal) {
		MappingHastusZdep mappingHastusZdep = new MappingHastusZdep();
		mappingHastusZdep.setReferential(ContextHolder.getContext());
		mappingHastusZdep.setZdep(zdep);
		mappingHastusZdep.setHastusOriginal(hastusoriginal);
		mappingHastusZdep.setHastusChouette(null);
		mappingHastusZdepDAO.create(mappingHastusZdep);
		return mappingHastusZdep;
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
		CommandFactory.factories.put(MappingZdepHastusPlageCommand.class.getName(), new DefaultCommandFactory());
	}
}
