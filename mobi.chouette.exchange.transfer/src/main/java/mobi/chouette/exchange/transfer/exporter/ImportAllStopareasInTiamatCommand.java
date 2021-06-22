package mobi.chouette.exchange.transfer.exporter;

import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ScheduledStopPointDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.updater.NeTExIdfmStopPlaceRegisterUpdater;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.Referential;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.hibernate.Hibernate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j
@Stateless(name = ImportAllStopareasInTiamatCommand.COMMAND)
public class ImportAllStopareasInTiamatCommand implements Command {

    public static final String COMMAND = "ImportAllStopareasInTiamatCommand";

    static {
        CommandFactory.factories.put(ImportAllStopareasInTiamatCommand.class.getName(), new DefaultCommandFactory());
    }

    @EJB
    private StopAreaDAO stopAreaDAO;

    @EJB(beanName = NeTExIdfmStopPlaceRegisterUpdater.BEAN_NAME)
    private NeTExIdfmStopPlaceRegisterUpdater neTExIdfmStopPlaceRegisterUpdater;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        String ref = ContextHolder.getContext();
        context.put("ref", ref);

        List<StopArea> stopAreas = stopAreaDAO.findAll();

        Referential referential = new Referential();

        List<List<StopArea>> stopAreasList = Lists.partition(stopAreas, 50);

        for(List<StopArea> stopAreas1 : stopAreasList){
            neTExIdfmStopPlaceRegisterUpdater.update(context, referential, stopAreas1);
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
}
