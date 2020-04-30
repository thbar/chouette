package mobi.chouette.exchange;

import java.util.HashMap;
import java.util.Map;

public class ProviderReferentialID {
    public static HashMap<String, String> providers = new HashMap<>();

    static {
        providers.put("CTVMI", "81");
        providers.put("CEOBUS", "84");
        providers.put("PERRIER", "42");
        providers.put("SQYBUS", "41");
        providers.put("STILE", "89");
        providers.put("TIMBUS", "87");
        providers.put("TVM", "86");
        providers.put("TEST", "12");
        providers.put("RD_BREST", "BIBUS");
        providers.put("RD_ANGERS", "IRIGO");
        providers.put("MOBICITEL40", "MOBICITEL40");
        providers.put("MOBICITE469", "MOBICITE469");
        providers.put("RDLA", "RDLA");
        providers.put("CTVH", "CTVH");
        providers.put("SAINT_MALO", "SAINT_MALO");
        providers.put("VALENCIENNES", "VALENCIENNES");
        generateSuperSpaceProviders();
    }

    private static void generateSuperSpaceProviders() {
        HashMap<String, String> providers = new HashMap<>();
        for (Map.Entry me : ProviderReferentialID.providers.entrySet()) {
            providers.put("MOSAIC_" + me.getKey().toString(), me.getValue().toString());
        }
        ProviderReferentialID.providers.putAll(providers);
    }
}
