package mobi.chouette.dao;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.model.Network;

import java.util.List;
import java.util.Optional;

@Stateless
public class NetworkDAOImpl extends GenericDAOImpl<Network> implements NetworkDAO{

	public NetworkDAOImpl() {
		super(Network.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	/**
	 * Get the network matching named passed as parameter
	 * @param name
	 * 		network's name
	 * @return
	 */
	@Override
	public Optional<Network> findByName(String name) {
		List<Network> resultList = em.createQuery("SELECT n FROM Network n WHERE n.name = :name", Network.class)
				.setParameter("name", name)
				.getResultList();

		return  resultList.stream().findFirst();
	}

}
