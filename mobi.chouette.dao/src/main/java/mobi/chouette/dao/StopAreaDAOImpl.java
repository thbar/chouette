package mobi.chouette.dao;

import mobi.chouette.core.ChouetteException;
import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class StopAreaDAOImpl extends GenericDAOImpl<StopArea> implements StopAreaDAO {

    public StopAreaDAOImpl() {
        super(StopArea.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<String> getBoardingPositionObjectIds() {
        return em.createQuery("select s.objectId from StopArea s where s.areaType = :areaType").setParameter("areaType", ChouetteAreaEnum.BoardingPosition).getResultList();
    }

    /**
     * Fusionner 2 stopAreas
     *
     * @param from
     * @param into
     * @throws CoreException
     */
    @Override
    public void mergeStopArea30m(Long from, Long into) throws CoreException {
        try {
            em.createNativeQuery(
                    "SELECT 1 FROM merge_duplicate_stop_area_30m( :safrom, :sainto)")
                    .setParameter("safrom", from)
                    .setParameter("sainto", into)
                    .getSingleResult();
        } catch (Exception e) {
            throw new CoreException(CoreExceptionCode.DELETE_IMPOSSIBLE,"Error while trying to merge point:" + from + " to point:" + into);
        }

    }

    @Override
    public void setModifiedFalseForAllStopAreas(String schema, List<String> stopAreas){
        em.createNativeQuery("UPDATE " + schema + ".stop_areas SET modified = FALSE WHERE objectid IN :objectIds")
                .setParameter("objectIds", stopAreas)
                .executeUpdate();
    }
}
