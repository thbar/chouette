package mobi.chouette.dao;

import mobi.chouette.model.Operator;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "OperatorDAO")
public class OperatorDAOImpl extends GenericDAOImpl<Operator> implements OperatorDAO {

    public OperatorDAOImpl() {
        super(Operator.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }
}
