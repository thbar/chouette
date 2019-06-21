package mobi.chouette.exchange.gtfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for network names and line transport modes.
 */

public class NetworksNames {
    private Map<String, String> producers = new HashMap<>();
    private List<String> producersExceptionNameNetwork = new ArrayList<>();
    private List<String> producersSitesTerritorialises = new ArrayList<>();

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
        producers.put("CAR","Cara'bus");
        producers.put("TUT","TuT");
//        producers.put("COU","Couralin");
        producers.put("PER","Péribus");
        producers.put("VIT","Vitalis");
        producers.put("YEG","Yego");
        producers.put("BER","TUB");
        producers.put("AGE","Tempo");
        producers.put("VDG","Evalys");
        producers.put("BDA","Baïa");
        producers.put("VIL","Elios");
        producers.put("LIB","Calibus");
        producers.put("MDM","TMA");
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
//        producers.put("LGA","Tidéo");
        producers.put("PAT","Transports64");
        producers.put("VIE","Lignes en Vienne");
        producers.put("BAC","Transports Maritimes Départementaux de la Gironde");
        producers.put("FAI","Liaison maritime Aix-Fouras");
//        producers.put("SNC","SNCF");
        producers.put("T1", "Test 1");
        producers.put("T2", "Test 2");
        producers.put("T3", "Test 3");


        producersExceptionNameNetwork.add("COU");
        producersExceptionNameNetwork.add("PBA");
        producersExceptionNameNetwork.add("SNC");
        producersExceptionNameNetwork.add("LGA");

        producersSitesTerritorialises.add("CHA");
        producersSitesTerritorialises.add("CMA");
        producersSitesTerritorialises.add("COR");
        producersSitesTerritorialises.add("CRE");
        producersSitesTerritorialises.add("DSE");
        producersSitesTerritorialises.add("DOR");
        producersSitesTerritorialises.add("GIR");
        producersSitesTerritorialises.add("HVI");
        producersSitesTerritorialises.add("LAN");
        producersSitesTerritorialises.add("LGA");
        producersSitesTerritorialises.add("PAT");
        producersSitesTerritorialises.add("VIE");


    }

    /**
     * Return the network name from the ID of the data space.
     * @param prefix
     * @return
     */
    public String getNetworkName(String prefix){
        return producers.get(prefix);
    }

    /**
     * Check if the ID of the data space is in the list.
     * @param prefix
     * @return
     */
    public Boolean getPrefixInList(String prefix){
        return producersExceptionNameNetwork.contains(prefix);
    }

    /**
     * Check if the data space identifier is not in the list.
     * @param prefix
     * @return
     */
    public Boolean getPrefixOutList(String prefix){
        return !producersExceptionNameNetwork.contains(prefix);
    }

    /**
     * Check that this is a territorial site.
     * @param prefix
     * @return
     */
    public Boolean getTerritorializedSites(String prefix){
        return producersSitesTerritorialises.contains(prefix);
    }

}
