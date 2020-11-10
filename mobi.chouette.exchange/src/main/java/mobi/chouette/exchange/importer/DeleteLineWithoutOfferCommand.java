package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.model.Line;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

@Stateless(name = DeleteLineWithoutOfferCommand.COMMAND)
@Log4j
public class DeleteLineWithoutOfferCommand implements Command, Constant {

    @EJB
    LineDAO lineDAO;

    public static final String COMMAND = "DeleteLineWithoutOfferCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        for(Line line : lineDAO.findAll()){
            if(line.getRoutes().size()==0)
                lineDAO.delete(line);
        }
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
        CommandFactory.factories.put(DeleteLineWithoutOfferCommand.class.getName(), new DeleteLineWithoutOfferCommand.DefaultCommandFactory());
    }

}
