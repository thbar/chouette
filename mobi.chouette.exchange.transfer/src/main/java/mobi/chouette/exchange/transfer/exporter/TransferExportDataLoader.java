package mobi.chouette.exchange.transfer.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AgencyDAO;
import mobi.chouette.dao.FeedInfoDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.OperatorDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.transfer.Constant;
import mobi.chouette.model.Agency;
import mobi.chouette.model.FeedInfo;
import mobi.chouette.model.Line;
import mobi.chouette.model.Operator;
import mobi.chouette.model.StopArea;
import org.jboss.ejb3.annotation.TransactionTimeout;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j
@Stateless(name = TransferExportDataLoader.COMMAND)
public class TransferExportDataLoader implements Command, Constant {

	public static final String COMMAND = "TransferExporterDataLoader";

	@EJB
	private AgencyDAO agencyDAO;

	@EJB
	private LineDAO lineDAO;

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB
	private OperatorDAO operatorDAO;

	@EJB
	private FeedInfoDAO feedInfoDAO;

	@PersistenceContext(unitName = "referential")
	private EntityManager em;

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@TransactionTimeout(value = 2, unit = TimeUnit.HOURS)
	public boolean execute(Context context) throws Exception {

		List<Agency> agenciesToTransfer = prepareAgencies();
		context.put(AGENCIES, agenciesToTransfer);
		List<Line> lineToTransfer = prepareLines(context);
		context.put(LINES, lineToTransfer);
	    List<StopArea> stopAreaToTransfer = prepareStopAreas(context);
		context.put(STOP_AREAS, stopAreaToTransfer);
		List<Operator> operatorToTransfer = prepareOperators(context);
		context.put(OPERATORS, operatorToTransfer);
		List<FeedInfo> feedInfosToTransfer = prepareFeedInfos();
		context.put(FEED_INFOS, feedInfosToTransfer);
		return true;
	}

	protected List<Line> prepareLines(Context context) throws IllegalArgumentException, SecurityException {
		if (!em.isJoinedToTransaction()) {
			throw new RuntimeException("No transaction");
		}
		
		TransferExportParameters configuration = (TransferExportParameters) context.get(CONFIGURATION);

		log.info("Loading all lines...");
		List<Line> allLines = lineDAO.findAll();
		
		List<Line> lineToTransfer = new ArrayList<>();
		
		LineFilter lineFilter = new LineFilter();

		log.info("Filtering lines");
		for (Line line : allLines) {
			// Clean according to date rules
			// Clean obsolete data
			boolean shouldKeep = lineFilter.filter(line, configuration.getStartDate(), configuration.getEndDate());

			if (shouldKeep) {
				lineToTransfer.add(line);
			}
		}
		
		log.info("Filtering lines completed");
		log.info("Removing Hibernate proxies");
		HibernateDeproxynator<?> deProxy = new HibernateDeproxynator<>();
		lineToTransfer = deProxy.deepDeproxy(lineToTransfer);
		log.info("Removing Hibernate proxies completed");
		

		em.clear();
		return lineToTransfer;
	}

	protected List<StopArea> prepareStopAreas(Context context) throws IllegalArgumentException, SecurityException {
		if (!em.isJoinedToTransaction()) {
			throw new RuntimeException("No transaction");
		}

		TransferExportParameters configuration = (TransferExportParameters) context.get(CONFIGURATION);

		log.info("Loading all stop areas...");
		List<StopArea> allStopAreas = stopAreaDAO.findAll();

		log.info("Removing Hibernate proxies");
		HibernateDeproxynator<?> deProxy = new HibernateDeproxynator<>();
		allStopAreas = deProxy.deepDeproxy(allStopAreas);
		log.info("Removing Hibernate proxies completed");


		em.clear();
		return allStopAreas;
	}

	protected List<Operator> prepareOperators(Context context) throws IllegalArgumentException, SecurityException {
		if (!em.isJoinedToTransaction()) {
			throw new RuntimeException("No transaction");
		}

		TransferExportParameters configuration = (TransferExportParameters) context.get(CONFIGURATION);

		log.info("Loading all operators...");
		List<Operator> operators = operatorDAO.findAll();

		log.info("Removing Hibernate proxies");
		HibernateDeproxynator<?> deProxy = new HibernateDeproxynator<>();
		operators = deProxy.deepDeproxy(operators);
		log.info("Removing Hibernate proxies completed");

		em.clear();
		return operators;
	}

	private List<FeedInfo> prepareFeedInfos() {
		if (!em.isJoinedToTransaction()) {
			throw new RuntimeException("No transaction");
		}

		log.info("Loading all feed infos...");
		List<FeedInfo> feedInfos = feedInfoDAO.findAll();

		log.info("Removing Hibernate proxies");
		HibernateDeproxynator<?> deProxy = new HibernateDeproxynator<>();
		feedInfos = deProxy.deepDeproxy(feedInfos);
		log.info("Removing Hibernate proxies completed");

		em.clear();
		return feedInfos;
	}

	private List<Agency> prepareAgencies() {
		if (!em.isJoinedToTransaction()) {
			throw new RuntimeException("No transaction");
		}

		log.info("Loading all agencies...");
		List<Agency> agencies = agencyDAO.findAll();

		log.info("Removing Hibernate proxies");
		HibernateDeproxynator<?> deProxy = new HibernateDeproxynator<>();
		agencies = deProxy.deepDeproxy(agencies);
		log.info("Removing Hibernate proxies completed");

		em.clear();
		return agencies;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.transfer/" + COMMAND;
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
		CommandFactory.factories.put(TransferExportDataLoader.class.getName(), new DefaultCommandFactory());
	}

}
