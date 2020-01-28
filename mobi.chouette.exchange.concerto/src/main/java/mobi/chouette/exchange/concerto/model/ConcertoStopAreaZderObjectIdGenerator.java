package mobi.chouette.exchange.concerto.model;

import mobi.chouette.model.StopArea;

public class ConcertoStopAreaZderObjectIdGenerator {
    public static ConcertoObjectId getConcertoObjectId(StopArea stop){
        ConcertoObjectId concertoObjectId = new ConcertoObjectId();
        try {
            concertoObjectId.setStif("STIF:StopPoint:Q:" + stop.getMappingHastusZdep().getZder() + ":");
        } catch(Exception e){
            concertoObjectId.setHastus(stop.getMappingHastusZdep().getHastusChouette());
            // le point nest pas idfm on zappe
            // @todo SCH que faire ? on zappe ? swallow ?
            // swallow
        }
        return concertoObjectId;
    }
}
