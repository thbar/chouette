package mobi.chouette.exchange.importer.updater;

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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdfmReflexParser {
    private static final Pattern patQuayId = Pattern.compile("Quay:([0-9]+)");
    private static final Pattern  patStopPlace = Pattern.compile("monomodalStopPlace:([0-9]+)");

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


    public static HashMap<String, Pair<String, String>> parseReflexResult(InputStream reflexResult) throws SaxonApiException, IOException {
        Processor proc = new Processor(false);
        XdmNode document = proc.newDocumentBuilder().build(new StreamSource(reflexResult));
        XPathCompiler xPathCompiler = proc.newXPathCompiler();
        xPathCompiler.declareNamespace("", "http://www.netex.org.uk/netex");

        //Grabb all ZDEP Quays
        String xpathRequestZdep = "//Quay[@derivedFromObjectRef]/@id";
        XPathExecutable getZdep = xPathCompiler.compile(xpathRequestZdep);
        XPathSelector zdepSelector = getZdep.load();
        zdepSelector.setContextItem(document);
        XdmValue xdmItemsZdep = zdepSelector.evaluate();

        HashMap<String, Pair<String, String>> dataFromXml = new HashMap<>();

        for (XdmItem it: xdmItemsZdep) {
            //Grabb ZDER of the CURRENT ZDEP
            String zdep = it.getStringValue();
            String xpathRequestZder = String.format("//Quay[contains(@id, '%s')]/@derivedFromObjectRef[1]", zdep);
            XPathExecutable getZderFromZdep = xPathCompiler.compile(xpathRequestZder);
            XPathSelector zderSelector = getZderFromZdep.load();
            zderSelector.setContextItem(document);
            XdmValue xdmItemsZder = zderSelector.evaluate();
            String zder =  xdmItemsZder.itemAt(0).getStringValue();

            //Grabb ZDLR of the CURRENT ZDER
            String xpathRequestZdlr = String.format("//Quay[contains(@id, '%s')]/ParentZoneRef/@ref[1]", zder);
            XPathExecutable getZdlrFromZder = xPathCompiler.compile(xpathRequestZdlr);
            XPathSelector zdlrSelector = getZdlrFromZder.load();
            zdlrSelector.setContextItem(document);
            XdmValue xdmItemsZdlr = zdlrSelector.evaluate();
            String zdlr =  xdmItemsZdlr.itemAt(0).getStringValue();

            String idZdep = getQuayIdFromXdmItem(it.getStringValue());
            String idZder = getQuayIdFromXdmItem(zder);
            String idZdlr = getStopPlaceIdFromXdmItem(zdlr);

              Pair<String, String> zderAndZdlr = new MutablePair<>(idZder, idZdlr);
              dataFromXml.put(idZdep, zderAndZdlr);
        }
        return dataFromXml;
    }

}
