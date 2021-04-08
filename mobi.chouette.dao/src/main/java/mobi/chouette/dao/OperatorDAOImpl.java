package mobi.chouette.dao;

import mobi.chouette.model.Line;
import mobi.chouette.model.Operator;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigInteger;
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
    public boolean hasOperators(String schema) {
        long count =  ((BigInteger) em.createNativeQuery("select COUNT(*) from " + schema + ".operators")
                .getSingleResult()).longValue();
        return count > 0;
    }
}
