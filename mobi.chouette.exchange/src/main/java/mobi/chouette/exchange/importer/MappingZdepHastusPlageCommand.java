package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.MappingHastusZdepDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.model.StopArea;
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
import java.util.Map;
import java.util.Optional;


@Log4j
@Stateless(name = MappingZdepHastusPlageCommand.COMMAND)
public class MappingZdepHastusPlageCommand implements Command {

	public static final String COMMAND = "MappingZdepHastusPlageCommand";

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB
	private MappingHastusZdepDAO mappingHastusZdepDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean execute(Context context) throws Exception {

		boolean result = ERROR;
		final Map<String, InputStream> inputStreamsByName = (Map<String, InputStream>) context.get("inputStreamByName");
		String inputStreamName = selectDataInputStreamName(inputStreamsByName);
		String hastus = null;
		String zdep;

		if (inputStreamName != null) {
			CSVParser csvParser = new CSVParser(new InputStreamReader(inputStreamsByName.get(inputStreamName)), CSVFormat.DEFAULT);
			int headerNbLine = 1;
			int cpt = 0;
			for (CSVRecord csvRecord : csvParser) {
				// entête
				if(cpt < headerNbLine) {
					cpt++;
					continue;
				}

				if(csvRecord.size() == 0) {
					throw new Exception("Format de fichier invalide");
				}

				zdep = csvRecord.get(0);
				if(zdep == null || zdep.length() == 0) continue;

				if(csvRecord.size() > 1) hastus = csvRecord.get(1);

				Optional<MappingHastusZdep> daoByZdep = mappingHastusZdepDAO.findByZdep(zdep);
				if (daoByZdep.isPresent()) {
					log.warn("Numéro ZDEP " + zdep + " déjà existant");
					continue;
				}

				GetObjectIdAndStopAreaFromHastus getObjectIdAndStopAreaFromHastus = new GetObjectIdAndStopAreaFromHastus(hastus).invoke();
				String objectId = getObjectIdAndStopAreaFromHastus.getObjectId();
				StopArea stopArea = getObjectIdAndStopAreaFromHastus.getStopArea();
				if(stopArea != null){
					if(stopArea.getMappingHastusZdep() != null){
						// On checke que c'est la bonne plage
						if(!stopArea.getMappingHastusZdep().getZdep().equals(zdep)){
							throw new Exception("INVALID_ZDEP_MAPPING");
						}
					} else {
						// On attache la zdep
						MappingHastusZdep hastusZdep = createMapping(zdep, hastus, objectId);
						stopArea.setMappingHastusZdep(hastusZdep);
						stopAreaDAO.update(stopArea);
					}
				} else {
					createMapping(zdep, hastus, null);
				}
			}
			result = SUCCESS;
		}

		return result;
	}

	private MappingHastusZdep createMapping(String zdep, String hastus, String objectId) {
		MappingHastusZdep mappingHastusZdep = new MappingHastusZdep();
		mappingHastusZdep.setReferential(ContextHolder.getContext());
		mappingHastusZdep.setZdep(zdep);
		mappingHastusZdep.setHastusOriginal(hastus);
		mappingHastusZdep.setHastusChouette(objectId);
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

	private class GetObjectIdAndStopAreaFromHastus {
		private String hastus;
		private String objectId;
		private StopArea stopArea;

		public GetObjectIdAndStopAreaFromHastus(String hastus) {
			this.hastus = hastus;
		}

		public String getObjectId() {
			return objectId;
		}

		public StopArea getStopArea() {
			return stopArea;
		}

		public GetObjectIdAndStopAreaFromHastus invoke() {
			if(hastus == null) return this;

			objectId = ContextHolder.getContext().toUpperCase() + ":BoardingPosition:" + hastus;
			stopArea = stopAreaDAO.findByObjectId(objectId);
			if(stopArea != null) return this;

			objectId = ContextHolder.getContext().toUpperCase() + ":Quay:" + hastus;
			stopArea = stopAreaDAO.findByObjectId(objectId);
			if(stopArea != null) return this;

			objectId = ContextHolder.getContext().toUpperCase() + ":CommercialStopPoint:" + hastus;
			stopArea = stopAreaDAO.findByObjectId(objectId);
			if(stopArea != null) return this;

			objectId = ContextHolder.getContext().toUpperCase() + ":StopPlace:" + hastus;
			stopArea = stopAreaDAO.findByObjectId(objectId);
			if(stopArea != null) return this;

			objectId = ContextHolder.getContext().toUpperCase() + ":ITL:" + hastus;
			stopArea = stopAreaDAO.findByObjectId(objectId);
			return this;
		}
	}
}
