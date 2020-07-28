package mobi.chouette.dao;

import mobi.chouette.model.Provider;
import mobi.chouette.model.ScheduledStopPoint;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless(name = "ProviderDAO")
public class ProviderDAOImpl extends GenericDAOImpl<Provider> implements ProviderDAO {

    public ProviderDAOImpl() {
        super(Provider.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<Provider> findBySchema(String schema) {
        List<Provider> providers = em.createNativeQuery("select * from admin.client c where c.schema_name ilike :schema", Provider.class).setParameter("schema", schema).getResultList();
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }
}
