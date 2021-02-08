package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.MappingHastusZdepDAO;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.exchange.importer.updater.IdfmReflexParser;
import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.model.Provider;
import org.apache.commons.lang3.tuple.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;


@Log4j
@Stateless(name = UpdateMappingZdepZderZdlrCommand.COMMAND)
public class UpdateMappingZdepZderZdlrCommand implements Command {

	public static final String COMMAND = "UpdateMappingZdepZderZdlrCommand";

	@EJB
	private MappingHastusZdepDAO mappingHastusZdepDAO;

	@EJB
	private ProviderDAO providerDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean execute(Context context) throws Exception {
		Boolean swallow = (Boolean) context.get("swallow");
		try {
			String referential = context.get("ref").toString().toUpperCase();
			log.info("provider: " + context.get("ref") + "provider referential: " + referential);
			Optional<Provider> provider = providerDAO.findBySchema(referential);
			String id = provider.orElseThrow(() -> new RuntimeException("Aucun provider trouvé avec pour schema " + referential)).getCodeIdfm();
			String requestHttpTarget = String.format(System.getProperty("iev.stop.place.zdep.zder.zdlr.mapping.by.ref"), id);
			log.info("provider: " + context.get("ref") + "provider id: " + id);
			log.info("http request ZDLR ZDER : " + requestHttpTarget + "id: " + id);
			InputStream input = new ByteArrayInputStream(PublicationDeliveryReflexService.getAll(requestHttpTarget));
			HashMap<String, Pair<String, String>> stringPairHashMap = IdfmReflexParser.parseReflexResult(input);
			log.info("Nombre ZDER ZDLR recuperes: " + stringPairHashMap.size());

			stringPairHashMap.forEach((zdep, zderZdlrPair) -> {
				Optional<MappingHastusZdep> byZdep = mappingHastusZdepDAO.findByZdep(zdep);
				if (byZdep.isPresent()) {
					MappingHastusZdep mappingHastusZdep = byZdep.get();
					mappingHastusZdep.setZder(zderZdlrPair.getLeft());
					mappingHastusZdep.setZdlr(zderZdlrPair.getRight());
				}
			});
			log.info("Les plages ZDEP de " + context.get("ref") + " ont été mappées à leurs ZDER et ZDLR");
		} catch (Exception e) {
            if (swallow == null || !swallow.booleanValue()) {
                throw e;
            }
        } finally {
            context.put("swallow", Boolean.FALSE);
        }
		return SUCCESS;
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
		CommandFactory.factories.put(UpdateMappingZdepZderZdlrCommand.class.getName(), new DefaultCommandFactory());
	}

}
