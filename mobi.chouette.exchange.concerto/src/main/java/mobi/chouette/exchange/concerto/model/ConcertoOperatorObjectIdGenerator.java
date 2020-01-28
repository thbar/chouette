package mobi.chouette.exchange.concerto.model;

public class ConcertoOperatorObjectIdGenerator {
    public static ConcertoObjectId getConcertoObjectId(String referential){
        ConcertoObjectId concertoObjectId = new ConcertoObjectId();
        switch(referential){
            case "sqybus":
                concertoObjectId.setStif("SQYBUS:Operator::SQYBUS");
                concertoObjectId.setHastus("SQYBUS:Company::410:LOC");
                break;
        }
        return concertoObjectId;
    }
}
