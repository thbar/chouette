package mobi.chouette.dao;

import mobi.chouette.model.Line;
import mobi.chouette.model.Operator;

import java.util.List;

public interface LineDAO extends GenericDAO<Line> {

    String updateStopareasForIdfmLineCommand(Long lineId) throws Exception;

    void mergeDuplicateJourneyPatternsOfLineAndAddSuffix(Long lineId, String lineName);

    List<Line> findByNetworkId(Long networkId);

    List<String> findObjectIdLinesInFirstDataspace(List<Long> ids, String dataspace);

    boolean hasLines(String schema);
}
