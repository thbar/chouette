package mobi.chouette.dao;

import mobi.chouette.model.Variations;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "VariationsDAO")
public class VariationsDAOImpl extends GenericDAOImpl<Variations> implements VariationsDAO {

    public VariationsDAOImpl() {
        super(Variations.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public void makeLineInsert(String type, String description, Long numJob) {
        Variations variation = new Variations();
        variation.setType(type);
        variation.setDescription(description);
        variation.setJob(numJob);
        this.create(variation);
    }

    @Override
    public void makeLineUpdate(String type, String description, Long numjob) {
        Variations variation = new Variations();
        variation.setType(type);
        variation.setDescription(description);
        variation.setJob(numjob);
        this.create(variation);
    }

}

