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

	public String updateStopareasForIdfmLineCommand(Long lineId) throws Exception
	{
		String retour = null;
		try {
			retour = (String) em.createNativeQuery("SELECT update_sa_for_idfm_line FROM update_sa_for_idfm_line(:lineId);")
					.setParameter("lineId", lineId)
					.getSingleResult();
		} catch (PersistenceException e){
			if(e.getCause().getCause().getMessage().contains("MOSAIC_SQL_ERROR:")){
				String[] splitError = e.getCause().getCause().getMessage().split("MOSAIC_SQL_ERROR:");
				throw new Exception("MOSAIC_SQL_ERROR:" + splitError[1]);
			} else {
				throw e;
			}
		} catch (Exception e) {
			throw e;
		}

		return retour;
	}

	@Override
	public void mergeDuplicateJourneyPatternsOfLineAndAddSuffix(Long lineId, String lineName) {
		em.createNativeQuery("SELECT merge_identicals_journey_patterns_for_line FROM merge_identicals_journey_patterns_for_line(:lineId, :lineName);")
				.setParameter("lineId", lineId)
				.setParameter("lineName", lineName)
				.getSingleResult();

		em.createNativeQuery("SELECT rename_identicals_journey_patterns_for_line FROM rename_identicals_journey_patterns_for_line(:lineId);")
				.setParameter("lineId", lineId)
				.getSingleResult();

	}
}
