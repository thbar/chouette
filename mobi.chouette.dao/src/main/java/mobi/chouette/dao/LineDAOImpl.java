package mobi.chouette.dao;

import mobi.chouette.model.Line;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

@Stateless (name="LineDAO")
public class LineDAOImpl extends GenericDAOImpl<Line> implements LineDAO {

	public LineDAOImpl() {
		super(Line.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	public String updateStopareasForIdfmLineCommand(String referential, Long lineId) throws Exception
	{
		String retour;
		try {
			retour = (String) em.createNativeQuery("SELECT update_sa_for_idfm_line FROM public.update_sa_for_idfm_line(:ref, :lineId);")
					.setParameter("ref", referential)
					.setParameter("lineId", lineId)
					.getSingleResult();
		} catch (PersistenceException e){
			throw new Exception(e.getCause());
		}

		return retour;
	}
	
}
