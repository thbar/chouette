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

    /**
     * Renvoie un MappingHastusZdep
     *  - si le mapping existe alors on renvoie le mapping
     *  - si non on renvoie le premier zdep libre
     * @param hastus
     * @return
     */
    @Override
    public Optional<MappingHastusZdep> findByHastus(String hastus) {
        List<MappingHastusZdep> resultList = em.createQuery("SELECT m FROM MappingHastusZdep m WHERE m.hastusOriginal = :hastus", MappingHastusZdep.class)
                .setParameter("hastus", hastus)
                .getResultList();
        if(resultList.size() == 0) {
            resultList = em.createQuery("SELECT m FROM MappingHastusZdep m WHERE COALESCE(m.hastusOriginal, '') LIKE '' AND COALESCE(m.hastusChouette, '') LIKE '' AND COALESCE(m.zdep, '') NOT LIKE ''", MappingHastusZdep.class)
                    .getResultList();
        }
        return  resultList.stream().findFirst();
    }
}
