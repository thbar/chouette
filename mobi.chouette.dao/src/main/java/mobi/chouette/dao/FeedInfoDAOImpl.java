package mobi.chouette.dao;

import mobi.chouette.model.FeedInfo;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless (name="FeedInfoDAO")
public class FeedInfoDAOImpl extends GenericDAOImpl<FeedInfo> implements FeedInfoDAO {

	public FeedInfoDAOImpl() {
		super(FeedInfo.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}
	
}
