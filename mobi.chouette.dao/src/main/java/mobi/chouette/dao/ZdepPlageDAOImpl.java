package mobi.chouette.dao;

import mobi.chouette.model.ZdepPlage;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless(name = "ZdepPlageDAO")
public class ZdepPlageDAOImpl extends GenericDAOImpl<ZdepPlage> implements ZdepPlageDAO {

    public ZdepPlageDAOImpl() {
        super(ZdepPlage.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public ZdepPlage findByZdep(String zdep) {
        List<ZdepPlage> resultList = em.createQuery("SELECT z FROM ZdepPlage z WHERE z.zdep = :zdep", ZdepPlage.class)
                .setParameter("zdep", zdep)
                .getResultList();
        if(!resultList.isEmpty())
            return resultList.get(0);
        return null;
    }
}
