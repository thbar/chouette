package mobi.chouette.dao;

import mobi.chouette.model.Provider;
import mobi.chouette.model.ScheduledStopPoint;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public List<Provider> getAllProviders() {
        return em.createNativeQuery("select * from admin.client c ", Provider.class).getResultList();
    }


    /**
     * Returns all providers schemas containing working data (schemas that does not start with mobiiti_xxxx)
     *
     * @return
     * 	The list of all schemas
     */
    @Override
    public List<String> getAllWorkingSchemas(){
        String superspacePrefix = System.getProperty("iev.superspace.prefix");

        List<Provider> providers = getAllProviders();
        return providers.stream().map(Provider::getSchemaName)
                .filter(schemaName -> !schemaName.startsWith(superspacePrefix))
                .collect(Collectors.toList());

    }
}
