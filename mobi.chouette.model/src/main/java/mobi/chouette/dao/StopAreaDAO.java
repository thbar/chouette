package mobi.chouette.dao;

import java.util.List;

import mobi.chouette.core.CoreException;
import mobi.chouette.model.StopArea;

public interface StopAreaDAO extends GenericDAO<StopArea> {

    List<String> getBoardingPositionObjectIds();
    void mergeStopArea30m(Long from, Long into) throws CoreException;
}
