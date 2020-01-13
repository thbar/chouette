package mobi.chouette.exchange.importer.updater;

import mobi.chouette.core.ChouetteException;
import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdfmReflexParser {
    private static final Pattern patQuayId = Pattern.compile("Quay:([0-9]+)");
    private static final Pattern patStopPlace = Pattern.compile("monomodalStopPlace:([0-9]+)");
    private static final String nameSpace = "http://www.netex.org.uk/netex";
    private static final String xpathRequestZdep = "//Quay[@derivedFromObjectRef]/@id";
    private static final String xpathZder = "//Quay[contains(@id, '%s')]/@derivedFromObjectRef[1]";
    private static final String xpathZdlr = "//Quay[contains(@id, '%s')]/ParentZoneRef/@ref[1]";

    public static HashMap<String, Pair<String, String>> parseReflexResult(InputStream reflexResult) throws ChouetteException {
        Processor proc = new Processor(false);
        XPathCompiler xPathCompiler = proc.newXPathCompiler();
        xPathCompiler.declareNamespace("", nameSpace);
        try {
            XdmNode document = proc.newDocumentBuilder().build(new StreamSource(reflexResult));
            //Grabb all ZDEP Quays
            XPathExecutable getZdep = xPathCompiler.compile(xpathRequestZdep);
            XPathSelector zdepSelector = getZdep.load();
            zdepSelector.setContextItem(document);
            XdmValue xdmItemsZdep = zdepSelector.evaluate();


            HashMap<String, Pair<String, String>> dataFromXml = new HashMap<>();

            for (XdmItem it : xdmItemsZdep) {
                String zdep = it.getStringValue();
                //Grabb ZDER of the CURRENT ZDEP
                String zder =  XMLGrabb(zdep, xpathZder, xPathCompiler, document);
                //Grabb ZDLR of the CURRENT ZDER
                String zdlr =  XMLGrabb(zder, xpathZdlr, xPathCompiler, document);
                String idZdep = getQuayIdFromXdmItem(it.getStringValue());
                String idZder = getQuayIdFromXdmItem(zder);
                String idZdlr = getStopPlaceIdFromXdmItem(zdlr);

                Pair<String, String> zderAndZdlr = new MutablePair<>(idZder, idZdlr);
                dataFromXml.put(idZdep, zderAndZdlr);
            }
            return dataFromXml;
        } catch (SaxonApiException e) {
            throw new CoreException(CoreExceptionCode.PARSING_EXCEPTION, e.getMessage());
        }
    }
    private static String XMLGrabb(String zd, String xPath, XPathCompiler xPathCompiler, XdmNode document) throws SaxonApiException{
        String xpathRequestZdtarget = String.format(xPath, zd);
        XPathExecutable getZd = xPathCompiler.compile(xpathRequestZdtarget);
        XPathSelector zdSelector = getZd.load();
        zdSelector.setContextItem(document);
        XdmValue xdmItemsZder = zdSelector.evaluate();
        return  xdmItemsZder.itemAt(0).getStringValue();
    }

    private static String getQuayIdFromXdmItem(String xdmItem) {
        Matcher m = patQuayId.matcher(xdmItem);
        String group = null;
        while (m.find()) {
            group = m.group(1);
        }
        return group;
    }

    private static String getStopPlaceIdFromXdmItem(String xdmItem) {
        Matcher m = patStopPlace.matcher(xdmItem);
        String group = null;
        while (m.find()) {
            group = m.group(1);
        }
        return group;
    }
}
