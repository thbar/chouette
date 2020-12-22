package mobi.chouette.dao;

import mobi.chouette.model.Operator;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless(name = "OperatorDAO")
public class OperatorDAOImpl extends GenericDAOImpl<Operator> implements OperatorDAO {

    public OperatorDAOImpl() {
        super(Operator.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Operator> findByReferential(String schema) {
        return em.createNativeQuery("select * from " + schema + ".operators", Operator.class)
                .getResultList();
    }
}
