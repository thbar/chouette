package mobi.chouette.exchange.gtfs.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.CompanyDAO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

@Log4j
@Stateless(name = DatabaseCompaniesCommand.COMMAND)
public class DatabaseCompaniesCommand implements Command, Constant {

    public static final String COMMAND = "DatabaseCompaniesCommand";

    @EJB
    private CompanyDAO companyDAO;

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        try {
            context.put(DB_COMPANIES, companyDAO.findAll());
            result = SUCCESS;
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        return result;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.gtfs/" + COMMAND;
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
        CommandFactory.factories.put(DatabaseCompaniesCommand.class.getName(), new DefaultCommandFactory());
    }
}
