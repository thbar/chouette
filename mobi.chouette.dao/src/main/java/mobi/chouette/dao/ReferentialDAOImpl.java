package mobi.chouette.dao;

import mobi.chouette.model.Referential;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Stateless
public class ReferentialDAOImpl extends GenericDAOImpl<Referential> implements ReferentialDAO {

    public ReferentialDAOImpl() { super(Referential.class); }

    @PersistenceContext(unitName = "public")
    EntityManager em;


//    @PersistenceContext(unitName = "public")
//    public void setEntityManager(EntityManager em) {
//        this.em = em;
//    }

    @Override
    public List<String> getReferentials() {
        Query query = em.createNativeQuery("SELECT SLUG FROM PUBLIC.REFERENTIALS");
        return query.getResultList();
    }

    @Override
    public String getReferentialNameBySlug(String slug) {
        String result = (String) em.createNativeQuery("SELECT name " +
                " FROM PUBLIC.REFERENTIALS " +
                "WHERE LOWER(slug) = :slug")
                .setParameter("slug", slug.toLowerCase())
                .getSingleResult();
        return result;
    }
}