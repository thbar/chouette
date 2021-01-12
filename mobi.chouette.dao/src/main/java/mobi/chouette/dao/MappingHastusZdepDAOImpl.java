package mobi.chouette.dao;

import mobi.chouette.model.MappingHastusZdep;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless(name = "MappingHastusZdepDAO")
public class MappingHastusZdepDAOImpl extends GenericDAOImpl<MappingHastusZdep> implements MappingHastusZdepDAO {

    public MappingHastusZdepDAOImpl() {
        super(MappingHastusZdep.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<MappingHastusZdep> findByZdep(String zdep) {
        List<MappingHastusZdep> resultList = em.createQuery("SELECT m FROM MappingHastusZdep m WHERE m.zdep = :zdep", MappingHastusZdep.class)
                .setParameter("zdep", zdep)
                .getResultList();
        return  resultList.stream().findFirst();
    }

    @Override
    public Optional<MappingHastusZdep> findByHastus(String hastus) {
        List<MappingHastusZdep> resultList = em.createQuery("SELECT m FROM MappingHastusZdep m WHERE m.hastusOriginal = :hastus", MappingHastusZdep.class)
                .setParameter("hastus", hastus)
                .getResultList();
        return  resultList.stream().findFirst();
    }
}
