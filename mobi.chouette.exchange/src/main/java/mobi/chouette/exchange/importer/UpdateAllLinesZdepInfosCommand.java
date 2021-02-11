package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

@Stateless(name = UpdateAllLinesZdepInfosCommand.COMMAND)
@Log4j
public class UpdateAllLinesZdepInfosCommand implements Command, Constant {

    @EJB
    LineDAO lineDAO;

    public static final String COMMAND = "UpdateAllLinesZdepInfosCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        // update zdep des nouveaux points
        lineDAO.findAll().forEach(line -> {
            if (line.getCategoriesForLine() != null && line.getCategoriesForLine().getName().equalsIgnoreCase("IDFM")) {
                try {
                    Command command = CommandFactory.create(new InitialContext(), UpdateStopareasForIdfmLineCommand.class.getName());
                    context.put(LINEID, line.getId());
                    command.execute(context);
                    context.remove(LINEID);
                } catch (Exception e) {
                    log.error("MAJ des zdep KO");
                }
            }
        });
        lineDAO.flush(); // to prevent SQL error outside method
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
        CommandFactory.factories.put(UpdateAllLinesZdepInfosCommand.class.getName(), new UpdateAllLinesZdepInfosCommand.DefaultCommandFactory());
    }

}
