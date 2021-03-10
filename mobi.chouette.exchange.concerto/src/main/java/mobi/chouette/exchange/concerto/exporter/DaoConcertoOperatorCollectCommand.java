package mobi.chouette.exchange.concerto.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.OperatorDAO;
import mobi.chouette.exchange.concerto.exporter.producer.ConcertoOperatorProducer;
import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import mobi.chouette.exchange.concerto.model.ConcertoOperator;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporter;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.model.Operator;
import org.codehaus.jettison.json.JSONException;
import org.joda.time.LocalDate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static mobi.chouette.exchange.concerto.Constant.CONCERTO_EXPORTER;
import static mobi.chouette.exchange.concerto.Constant.CONCERTO_OPERATORS;

@Log4j
@Stateless(name = DaoConcertoOperatorCollectCommand.COMMAND)
public class DaoConcertoOperatorCollectCommand implements Command, Constant {

    public static final String COMMAND = "DaoConcertoOperatorCollectCommand";

    private final String TYPE = "operator";

    @EJB
    private OperatorDAO operatorDAO;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        try {

            collectOperators(context);

            result = SUCCESS;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }

        return result;
    }

    private void collectOperators(Context context) throws JAXBException, JSONException {
        ConcertoExporter exporter = (ConcertoExporter) context.get(CONCERTO_EXPORTER);
        ConcertoExportParameters parameters = (ConcertoExportParameters) context.get(CONFIGURATION);
        String objectTypeConcerto = (String) context.get(OBJECT_TYPE_CONCERTO);
        ConcertoOperatorProducer operatorProducer = new ConcertoOperatorProducer(exporter);

        LocalDate startDate = operatorProducer.getStartDate(parameters);
        LocalDate endDate = operatorProducer.getEndDate(parameters, startDate);

        List<ConcertoOperator> concertoOperators = new ArrayList<>();

        List<Operator> operators = operatorDAO.findAll();


        for(Operator operator : operators){
            ConcertoObjectId objectId = new ConcertoObjectId();
            objectId.setStif(operator.getStifValue());
            objectId.setHastus(operator.getHastusValue());
            String concertoObjectId = operatorProducer.getObjectIdConcerto(objectId, objectTypeConcerto);

            UUID uuid = UUID.randomUUID();

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                ConcertoOperator concertoOperator = save(date, operator, concertoObjectId, uuid);
                concertoOperators.add(concertoOperator);
            }
        }

        if (context.get(CONCERTO_OPERATORS) != null) {
            concertoOperators.addAll((List<ConcertoOperator>) context.get(CONCERTO_OPERATORS));
        }
        context.put(CONCERTO_OPERATORS, concertoOperators);
    }

    private ConcertoOperator save(LocalDate date, Operator operator, String objectId, UUID uuid){
        ConcertoOperator concertoOperator = new ConcertoOperator();
        concertoOperator.setType(TYPE);
        concertoOperator.setUuid(uuid);
        concertoOperator.setDate(date);
        concertoOperator.setObjectId(objectId);
        concertoOperator.setName(operator.getName());

        return concertoOperator;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.concerto/"
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
        CommandFactory.factories.put(DaoConcertoOperatorCollectCommand.class.getName(),
                new DaoConcertoOperatorCollectCommand.DefaultCommandFactory());
    }

}
