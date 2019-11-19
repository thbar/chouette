package mobi.chouette.dao;

import mobi.chouette.model.AccessPoint;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AccessPointDAOImpl extends GenericDAOImpl<AccessPoint> implements AccessPointDAO{

	public AccessPointDAOImpl() {
		super(AccessPoint.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

}
