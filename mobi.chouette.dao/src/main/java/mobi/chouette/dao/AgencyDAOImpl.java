package mobi.chouette.dao;

import mobi.chouette.model.Agency;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless (name="AgencyDAO")
public class AgencyDAOImpl extends GenericDAOImpl<Agency> implements AgencyDAO {

	public AgencyDAOImpl() {
		super(Agency.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}
	
}
