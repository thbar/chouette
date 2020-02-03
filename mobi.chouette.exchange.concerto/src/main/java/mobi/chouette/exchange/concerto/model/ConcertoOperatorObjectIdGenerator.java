package mobi.chouette.exchange.concerto.model;

public class ConcertoOperatorObjectIdGenerator {
    public static ConcertoObjectId getConcertoObjectId(String companyName){
        ConcertoObjectId concertoObjectId = new ConcertoObjectId();
        switch(companyName.toUpperCase()){
            case "CARS PERRIER":
                concertoObjectId.setStif("PERRIER:Operator::PERRIER");
                concertoObjectId.setHastus("410");
                break;
            case "CEOBUS OISE":
                concertoObjectId.setStif("CEOBUS:Operator::CEOBUS_OISE");
                concertoObjectId.setHastus("CEOBUS:Operator::Oise:LOC");
                break;
            case "CEOBUS SCOLAIRE":
                concertoObjectId.setStif("CEOBUS:Operator::CEOBUS_SCOLAIRE");
                concertoObjectId.setHastus("CEOBUS:Operator::Scolaire:LOC");
                break;
            case "CEOBUS VEXIN":
                concertoObjectId.setStif("CEOBUS:Operator::CEOBUS_VEXIN");
                concertoObjectId.setHastus("CEOBUS:Operator::Vexin:LOC");
                break;
            case "CTVMI CSO A14":
                concertoObjectId.setStif("CTVMI:Operator::CTVMI_CSO_A14");
                concertoObjectId.setHastus("CTVMI:Operator::ENTREPRISE - CSO A14:LOC");
                break;
            case "CTVMI HOUDANAIS":
                concertoObjectId.setStif("CTVMI:Operator::CTVMI_HOUDANAIS");
                concertoObjectId.setHastus("CTVMI:Operator::CT3-40-HOUDANAIS:LOC");
                break;
            case "CTVMI L9511":
                concertoObjectId.setStif("CTVMI:Operator::CTVMI_L9511");
                concertoObjectId.setHastus("CTVMI:Operator::TIMBUS - Ligne 95-11:LOC");
                break;
            case "CTVMI MOBILIEN":
                concertoObjectId.setStif("CTVMI:Operator::CTVMI_MOBILIEN");
                concertoObjectId.setHastus("CTVMI:Operator::CT3-75-MOBILIEN:LOC");
                break;
            case "CTVMI PERIURBAIN MANTES":
                concertoObjectId.setStif("CTVMI:Operator::CTVMI_PU_MANTES");
                concertoObjectId.setHastus("CTVMI:Operator::CT3-33-PERIURBAIN MANTES:LOC");
                break;
            case "CTVMI POISSY AVAL":
                concertoObjectId.setStif("CTVMI:Operator::CTVMI_POISSY_AVAL");
                concertoObjectId.setHastus("CTVMI:Operator::CT3-20-POISSY AVAL:LOC");
                break;
            case "CTVMI POISSY AVAL L7":
                concertoObjectId.setStif("CTVMI:Operator::CTVMI_POISSY_AVAL_L7");
                concertoObjectId.setHastus("CTVMI:Operator::CT3-20-POISSY AVAL ligne 7:LOC");
                break;
            case "CTVMI TAM LIMAY":
                concertoObjectId.setStif("CTVMI:Operator::CTVMI_TAM_LIMAY");
                concertoObjectId.setHastus("CTVMI:Operator::CT3-41-TAM LIMAY:LOC");
                break;
            case "MOBICITE":
                concertoObjectId.setStif("MOBICITE:Operator::MOBICITE");
                concertoObjectId.setHastus("L40");
                break;
            case "SQYBUS":
                concertoObjectId.setStif("SQYBUS:Operator::SQYBUS");
                concertoObjectId.setHastus("SQYBUS:Company::410:LOC");
                break;
            case "STILE":
                concertoObjectId.setStif("STILE:Operator::STILE");
                concertoObjectId.setHastus("STILE:Operator::STILE:LOC");
                break;
            case "TIMBUS":
                concertoObjectId.setStif("TIMBUS:Operator::TIMBUS");
                concertoObjectId.setHastus("TIMBUS:Operator::TIMBUS:LOC");
                break;
            case "TVM":
                concertoObjectId.setStif("TVM:Operator::TVM");
                concertoObjectId.setHastus("TVM:Operator::TVM:LOC");
                break;
            default: // nothing
        }
        return concertoObjectId;
    }
}
