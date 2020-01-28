package mobi.chouette.dao;

import mobi.chouette.model.Route;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopPoint;
import org.hibernate.Hibernate;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;
@Stateless
public class ScheduledStopPointDAOImpl extends GenericDAOImpl<ScheduledStopPoint> implements ScheduledStopPointDAO {

	public ScheduledStopPointDAOImpl() {
		super(ScheduledStopPoint.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	@Override
	//@todo SCH voir comment les datas s'initialisent sans faire d'hibernate initialize sur un findall sale
	public List<ScheduledStopPoint> findAll()
	{
		List<ScheduledStopPoint> all = super.findAll();
		for (ScheduledStopPoint scheduledStopPoint : all) {
			Hibernate.initialize(scheduledStopPoint.getStopPoints());
			Hibernate.initialize(scheduledStopPoint.getContainedInStopAreaRef());
			for (StopPoint stopPoint : scheduledStopPoint.getStopPoints()) {
				Hibernate.initialize(stopPoint.getRoute());
				Route route = stopPoint.getRoute();
				Hibernate.initialize(route.getLine());
				route.getObjectId();
			}
		}
		return all;
	}

	@Override
	public List<ScheduledStopPoint> getScheduledStopPointsContainedInStopArea(String stopAreaObjectId) {
		return em.createQuery("select ssp from ScheduledStopPoint ssp where ssp.containedInStopAreaObjectId=:stopAreaObjectId", ScheduledStopPoint.class).setParameter("stopAreaObjectId", stopAreaObjectId).getResultList();
	}

	@Override
	public int replaceContainedInStopAreaReferences(Set<String> oldStopAreaIds, String newStopAreaId) {
		if (oldStopAreaIds != null && oldStopAreaIds.size() > 0) {
			return em.createQuery("update ScheduledStopPoint ssp set ssp.containedInStopAreaObjectId=:newStopAreaId where " +
					"ssp.containedInStopAreaObjectId in (:oldStopAreaIds)").setParameter("oldStopAreaIds", oldStopAreaIds).setParameter("newStopAreaId", newStopAreaId).executeUpdate();
		}
		return 0;
	}

	/**
	 * Get in separate transactions in order to be able to iterate over all referentials
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override
	public List<String> getAllStopAreaObjectIds() {
		return em.createQuery("select distinct(ssp.containedInStopAreaObjectId) from ScheduledStopPoint ssp where ssp.containedInStopAreaObjectId is not null", String.class).getResultList();
	}
}
