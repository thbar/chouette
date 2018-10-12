package mobi.chouette.exchange.gtfs;

import java.util.HashMap;
import java.util.Map;

public class NetworksNames {
    private Map<String, String> producers = new HashMap<>();

    public NetworksNames(){
        //AOM
        producers.put("BME","TBM");
        producers.put("BRI","Libéo");
        producers.put("CHL","TAC");
        producers.put("ANG","STGA");
        producers.put("COG","Transcom");
        producers.put("LRO","Yélo");
        producers.put("LIM","TCL");
        producers.put("NIO","TANlib");
        producers.put("PAU","Idelis");
        producers.put("ROC","R'Bus");
        producers.put("ROY","Cara'bus");
        producers.put("TUT","TuT");
        producers.put("COU","Couralin");
        producers.put("PER","Péribus");
        producers.put("VIT","Vitalis");
        producers.put("MAC","Yego");
        producers.put("BER","TUB");
        producers.put("AGE","Tempo");
        producers.put("VDG","Evalys");
        producers.put("BDA","Baïa");
        producers.put("VIL","Elios");
        producers.put("LIB","Calibus");
        producers.put("MAR","TMA");
//        producers.put("PBA","Réseau Pays Basque(Chronoplus et Hegobus)"); // à voir
        producers.put("GUE","agglo'Bus");
        producers.put("BBR","agglo2B");
        producers.put("OLE","Liaison maritime Oléron-La Rochelle");
        producers.put("SAI","BUSS");

        //Sites territorialisés
        producers.put("CHA","réseau interurbain 16");
        producers.put("CMA","réseau interurbain 17");
        producers.put("COR","réseau interurbain 19");
        producers.put("CRE","TransCreuse");
        producers.put("DSE","RDS");
        producers.put("DOR","TransPérigord");
        producers.put("GIR","TransGironde");
        producers.put("HVI","Moohv87");
        producers.put("LAN","XL'R");
        producers.put("LGA","Tidéo");
        producers.put("PAT","Transports64");
        producers.put("VIE","Lignes en Vienne");
        producers.put("BAC","Transports Maritimes Départementaux de la Gironde");
        producers.put("FAI","Liaison maritime Aix-Fouras");
//        producers.put("SNC","SNCF");
    }

    public String getNetworkName(String prefix){
        return producers.get(prefix);
    }

}
