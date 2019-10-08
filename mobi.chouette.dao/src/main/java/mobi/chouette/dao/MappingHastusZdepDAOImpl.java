package mobi.chouette.dao;

import mobi.chouette.model.MappingHastusZdep;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless(name = "MappingHastusZdepDAO")
public class MappingHastusZdepDAOImpl extends GenericDAOImpl<MappingHastusZdep> implements MappingHastusZdepDAO {

    public MappingHastusZdepDAOImpl() {
        super(MappingHastusZdep.class);
    }

    @PersistenceContext(unitName = "public")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    protected String getTableName() {
        return "public." + super.getTableName();
    }

    @Override
    public MappingHastusZdep findByZdep(String zdep) {
        List<MappingHastusZdep> resultList = em.createQuery("SELECT m FROM MappingHastusZdep m WHERE m.zdep = :zdep", MappingHastusZdep.class)
                .setParameter("zdep", zdep)
                .getResultList();
        if(!resultList.isEmpty())
            return resultList.get(0);
        return null;
    }
}
