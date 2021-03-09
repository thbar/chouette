package mobi.chouette.exchange.gtfs.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.model.Line;
import mobi.chouette.persistence.hibernate.ContextHolder;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = MappingLinesIdsCommand.COMMAND)
public class MappingLinesIdsCommand implements Command, Constant {

    public static final String COMMAND = "MappingLinesIdsCommand";

    @EJB
    private LineDAO lineDAO;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        Monitor monitor = MonitorFactory.start(COMMAND);

        try {

            GtfsExportParameters parameters = (GtfsExportParameters) context.get(CONFIGURATION);

            if (parameters.getIds() != null) {
                String currentShema = ContextHolder.getContext();
                List<String> objectIds = lineDAO.findObjectIdLinesInFirstDataspace(parameters.getIds(), currentShema.replace("mosaic_", ""));

                List<Line> superSpaceLines = lineDAO.findAll();

                List<Long> ids = new ArrayList<>();
                for (Line superSpaceLine : superSpaceLines) {
                    for (String objectId : objectIds){
                        if(superSpaceLine.getObjectId().equals(objectId)){
                            ids.add(superSpaceLine.getId());
                        }
                    }
                }
                parameters.setIds(ids);
            }

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
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.gtfs/"
                        + COMMAND;
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
        CommandFactory.factories.put(MappingLinesIdsCommand.class.getName(),
                new MappingLinesIdsCommand.DefaultCommandFactory());
    }

}
