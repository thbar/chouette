package mobi.chouette.exchange.concerto.model;

public class ConcertoLineObjectIdGenerator {
    public static ConcertoObjectId getConcertoObjectId(String codifLigne, String hastusId){
        ConcertoObjectId concertoObjectId = new ConcertoObjectId();
        concertoObjectId.setStif("STIF:Line::" + codifLigne+":");
        concertoObjectId.setHastus(hastusId);
        return concertoObjectId;
    }
}
