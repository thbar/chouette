package mobi.chouette.exchange.neptune.jaxb;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * Prefix mapper to have pretty namespace in xml instead of ns1,ns2,...
 *
 */
public class NeptuneNamespacePrefixMapper extends NamespacePrefixMapper {

    private static final String TRIDENT_PREFIX = ""; // DEFAULT NAMESPACE
    private static final String TRIDENT_URI = "http://www.trident.org/schema/trident";

    private static final String SIRI_PREFIX = "siri";
    private static final String SIRI_URI = "http://www.siri.org.uk/siri";

    private static final String IFOPT_PREFIX = "acsb";
    private static final String IFOPT_URI = "http://www.ifopt.org.uk/acsb";

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if (TRIDENT_URI.equals(namespaceUri)) {
            return TRIDENT_PREFIX;
        } else if (SIRI_URI.equals(namespaceUri)) {
            return SIRI_PREFIX;
        } else if (IFOPT_URI.equals(namespaceUri)) {
            return IFOPT_PREFIX;
        }
        return suggestion;
    }
}
