package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AccessLinkDAO;
import mobi.chouette.dao.AccessPointDAO;
import mobi.chouette.dao.BookingArrangementDAO;
import mobi.chouette.dao.BrandingDAO;
import mobi.chouette.dao.CategoriesForLinesDAO;
import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.dao.ConnectionLinkDAO;
import mobi.chouette.dao.ContactStructureDAO;
import mobi.chouette.dao.DestinationDisplayDAO;
import mobi.chouette.dao.FeedInfoDAO;
import mobi.chouette.dao.FlexibleServicePropertiesDAO;
import mobi.chouette.dao.FootnoteDAO;
import mobi.chouette.dao.GroupOfLineDAO;
import mobi.chouette.dao.InterchangeDAO;
import mobi.chouette.dao.JourneyFrequencyDAO;
import mobi.chouette.dao.JourneyPatternDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.MappingHastusZdepDAO;
import mobi.chouette.dao.NetworkDAO;
import mobi.chouette.dao.OperatorDAO;
import mobi.chouette.dao.RouteDAO;
import mobi.chouette.dao.RoutePointDAO;
import mobi.chouette.dao.RouteSectionDAO;
import mobi.chouette.dao.ScheduledStopPointDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.dao.StopPointDAO;
import mobi.chouette.dao.TimebandDAO;
import mobi.chouette.dao.TimetableDAO;
import mobi.chouette.dao.VehicleJourneyAtStopDAO;
import mobi.chouette.dao.VehicleJourneyDAO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

@Log4j
@Stateless(name = CleanRepositoryCommand.COMMAND)
public class CleanRepositoryCommand implements Command {

	public static final String COMMAND = "CleanRepositoryCommand";

	@EJB
	private CompanyDAO companyDAO;

	@EJB
	private GroupOfLineDAO groupOfLineDAO;

	@EJB
	private JourneyFrequencyDAO journeyFrequencyDAO;

	@EJB
	private JourneyPatternDAO journeyPatternDAO;

	@EJB
	private LineDAO lineDAO;

	@EJB
	private NetworkDAO networkDAO;

	@EJB
	private RouteDAO routeDAO;

	@EJB
	private RouteSectionDAO routeSectionDAO;

	@EJB
	private StopPointDAO stopPointDAO;

	@EJB
	private ScheduledStopPointDAO scheduledStopPointDAO;

	@EJB
	private TimetableDAO timetableDAO;

	@EJB
	private TimebandDAO timebandDAO;

	@EJB
	private VehicleJourneyDAO vehicleJourneyDAO;

	@EJB
	private VehicleJourneyAtStopDAO vehicleJourneyAtStopDAO;

	@EJB
	private DestinationDisplayDAO destinationDisplayDAO;

	@EJB
	private FootnoteDAO footnoteDAO;

	@EJB
	private BrandingDAO brandingDAO;

	@EJB
	private InterchangeDAO interchangeDAO;

	@EJB
	private RoutePointDAO routePointDAO;

	@EJB
	private ContactStructureDAO contactStructureDAO;

	@EJB
	private BookingArrangementDAO bookingArrangementDAO;

	@EJB
	private FlexibleServicePropertiesDAO flexibleServicePropertiesDAO;

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB
	AccessLinkDAO accessLinkDao;

	@EJB
	AccessPointDAO accessPointDAO;

	@EJB
	ConnectionLinkDAO connectionLinkDAO;

	@EJB
	MappingHastusZdepDAO mappingHastusZdepDAO;

	@EJB
	CategoriesForLinesDAO categoriesForLinesDAO;

	@EJB
	OperatorDAO operatorDAO;

	@EJB
	FeedInfoDAO feedInfoDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean execute(Context context) throws Exception {

		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		try {

			companyDAO.truncate();

			groupOfLineDAO.truncate();
			journeyFrequencyDAO.truncate();
			journeyPatternDAO.truncate();
			lineDAO.truncate();
			networkDAO.truncate();
			routeDAO.truncate();
			routeSectionDAO.truncate();
			footnoteDAO.truncate();
			brandingDAO.truncate();
			stopPointDAO.truncate();
			scheduledStopPointDAO.truncate();
			timetableDAO.truncate();
			timebandDAO.truncate();
			vehicleJourneyDAO.truncate();
			vehicleJourneyAtStopDAO.truncate();
			destinationDisplayDAO.truncate();
			interchangeDAO.truncate();
			routePointDAO.truncate();
			flexibleServicePropertiesDAO.truncate();
			bookingArrangementDAO.truncate();
			contactStructureDAO.truncate();
			accessLinkDao.truncate();
			accessPointDAO.truncate();
			connectionLinkDAO.truncate();
			stopAreaDAO.truncate();

			mappingHastusZdepDAO.truncate();
			if(context != null && context.containsKey(CLEAR_TABLE_CATEGORIES_FOR_LINES) && context.get(CLEAR_TABLE_CATEGORIES_FOR_LINES) == Boolean.TRUE) {
				categoriesForLinesDAO.truncate();
				operatorDAO.truncate();
				feedInfoDAO.truncate();
			}

			result = SUCCESS;
		} catch (Exception e) {
			log.error(e);
			throw e;
		}
		log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
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
		CommandFactory.factories.put(CleanRepositoryCommand.class.getName(), new DefaultCommandFactory());
	}
}
