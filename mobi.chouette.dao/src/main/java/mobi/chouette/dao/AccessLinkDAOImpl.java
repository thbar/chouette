package mobi.chouette.dao;

import mobi.chouette.model.AccessLink;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AccessLinkDAOImpl extends GenericDAOImpl<AccessLink> implements AccessLinkDAO {

    public AccessLinkDAOImpl() {
        super(AccessLink.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
