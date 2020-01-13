package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.MappingHastusZdepDAO;
import mobi.chouette.exchange.ProviderReferentialID;
import mobi.chouette.exchange.importer.updater.IdfmReflexParser;
import mobi.chouette.model.MappingHastusZdep;
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
	private static final String ievPropUrlServiceTarget = "iev.stop.place.zdep.zder.zdlr.mapping.by.ref";
	@EJB
	private MappingHastusZdepDAO mappingHastusZdepDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		String id = ProviderReferentialID.providers.get(context.get("ref").toString().toUpperCase());
		String requestHttpTarget = String.format(System.getProperty(ievPropUrlServiceTarget), id);
		log.info("provider: " + context.get("ref") + "provider id: " + id);
		log.info("http request : " + requestHttpTarget+ "id: " + id);
		InputStream input = new ByteArrayInputStream(PublicationDeliveryReflexService.getAll(requestHttpTarget));
		HashMap<String, Pair<String, String>> stringPairHashMap = IdfmReflexParser.parseReflexResult(input);

		stringPairHashMap.forEach((zdep, zderZdlrPair) -> {
			Optional<MappingHastusZdep> byZdep = mappingHastusZdepDAO.findByZdep(zdep);
			if (byZdep.isPresent()) {
				MappingHastusZdep mappingHastusZdep = byZdep.get();
				mappingHastusZdep.setZder(zderZdlrPair.getLeft());
				mappingHastusZdep.setZdlr(zderZdlrPair.getRight());
			}
		});
		log.info("zdeps of " + context.get("ref") + " are now updated and linked to their respectiv zder and zdlr on database!");
		return result;
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
