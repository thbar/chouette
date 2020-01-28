package mobi.chouette.dao;

import mobi.chouette.model.CategoriesForLines;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "CategoriesForLinesDAO")
public class CategoriesForLinesDAOImpl extends GenericDAOImpl<CategoriesForLines> implements CategoriesForLinesDAO {

    public CategoriesForLinesDAOImpl() {
        super(CategoriesForLines.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }
}
