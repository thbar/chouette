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
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Callable;

@Log4j
@Stateless(name = UpdateMappingZdepZderZdlrAsynchronousCommand.COMMAND)
public class UpdateMappingZdepZderZdlrAsynchronousCommand implements Command {

    public static final String COMMAND = "UpdateMappingZdepZderZdlrAsynchronousCommand";

    static {
        CommandFactory.factories.put(UpdateMappingZdepZderZdlrAsynchronousCommand.class.getName(), new DefaultCommandFactory());
    }

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    ManagedExecutorService executor;

    @EJB
    private ProviderDAO providerDAO;

    @EJB
    private MappingHastusZdepDAO mappingHastusZdepDAO;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {

        CommandCallableToUpdateZDLr callableZDLr = new UpdateMappingZdepZderZdlrAsynchronousCommand.CommandCallableToUpdateZDLr();
        Optional<Provider> provider = providerDAO.findBySchema(ContextHolder.getContext());
        callableZDLr.codeIdfm = provider.orElseThrow(() -> new RuntimeException("Aucun provider trouvé avec pour schema " + ContextHolder.getContext())).getCodeIdfm();
        callableZDLr.context = ContextHolder.getContext();
        executor.submit(callableZDLr);

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

    private class CommandCallableToUpdateZDLr implements Callable<Void> {
        private String codeIdfm;
        private String context;

        @Override
        @TransactionAttribute(TransactionAttributeType.REQUIRED)
        public Void call() throws Exception {
            ContextHolder.setContext(this.context);
            String requestHttpTarget = String.format(System.getProperty("iev.stop.place.zdep.zder.zdlr.mapping.by.ref"), this.codeIdfm);
            log.info("http request ZDLR ZDER : " + requestHttpTarget + "id: " + codeIdfm);
            InputStream input = new ByteArrayInputStream(PublicationDeliveryReflexService.getAll(requestHttpTarget));
            HashMap<String, Pair<String, String>> stringPairHashMap = IdfmReflexParser.parseReflexResult(input);
            log.info("Nombre ZDER ZDLR recuperes: " + stringPairHashMap.size());

            stringPairHashMap.forEach((zdep, zderZdlrPair) -> {
                Optional<MappingHastusZdep> byZdep = mappingHastusZdepDAO.findByZdep(zdep);
                if (byZdep.isPresent()) {
                    MappingHastusZdep mappingHastusZdep = byZdep.get();
                    mappingHastusZdep.setZder(zderZdlrPair.getLeft());
                    mappingHastusZdep.setZdlr(zderZdlrPair.getRight());
                    mappingHastusZdepDAO.update(mappingHastusZdep);
                }
            });

            return null;
        }
    }
}

