package mobi.chouette.exchange.concerto.model;

import mobi.chouette.model.StopArea;

public class ConcertoStopAreaZdlrObjectIdGenerator {
    public static ConcertoObjectId getConcertoObjectId(StopArea stop){
        ConcertoObjectId concertoObjectId = new ConcertoObjectId();
        try {
            concertoObjectId.setStif("STIF:StopArea:SP:" + stop.getMappingHastusZdep().getZdlr() + ":");
            // ie {"stif":"STIF:StopArea:SP:57769:"}
            // 1a584c21-3935-44ed-98c4-fe4005db53c9
        } catch(Exception e){
            concertoObjectId.setHastus(stop.getMappingHastusZdep().getHastusChouette());
            // le point nest pas idfm on zappe
            // @todo SCH que faire ? on zappe ? swallow ?
            // swallow
        }
        return concertoObjectId;
    }
}
