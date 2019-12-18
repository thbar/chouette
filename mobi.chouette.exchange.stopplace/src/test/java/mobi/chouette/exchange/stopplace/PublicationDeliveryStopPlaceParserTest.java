package mobi.chouette.exchange.stopplace;

import com.google.common.collect.Sets;

import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.netexprofile.util.NetexObjectUtil;
import mobi.chouette.model.StopArea;

import net.sf.saxon.s9api.*;
import org.rutebanken.netex.model.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.internal.collections.Pair;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static javax.xml.bind.JAXBContext.newInstance;

public class PublicationDeliveryStopPlaceParserTest {

    @Test
    public void testParseStopPlacesFromPublicationDelivery() throws Exception {
        Set<String> expectedActiveIds = Sets.newHashSet("NSR:StopPlace:51566", "NSR:StopPlace:11001");
        Set<String> expectedRemovedIds = Sets.newHashSet("NSR:StopPlace:10089");
        PublicationDeliveryStopPlaceParser parser = new PublicationDeliveryStopPlaceParser(new FileInputStream("src/test/resources/netex/PublicationDeliveryWithStopPlaces.xml"));

        Collection<StopArea> activeStopAreas = parser.getUpdateContext().getActiveStopAreas();
        Assert.assertEquals(activeStopAreas.size(), expectedActiveIds.size());
        Assert.assertTrue(activeStopAreas.stream().allMatch(sa -> expectedActiveIds.remove(sa.getObjectId())));

        Collection<String> inactiveStopAreas = parser.getUpdateContext().getInactiveStopAreaIds();
        Assert.assertEquals(inactiveStopAreas.size(), expectedRemovedIds.size());
        Assert.assertTrue(inactiveStopAreas.stream().allMatch(id -> expectedRemovedIds.remove(id)));

        Map<String, Set<String>> mergedQueys = parser.getUpdateContext().getMergedQuays();
        Assert.assertEquals(mergedQueys.size(), 4);
        Assert.assertEquals(mergedQueys.get("NSR:Quay:11001a"), Sets.newHashSet("NSR:Quay:11002", "NSR:Quay:11003", "SKY:Quay:12348413"), "Quays should have been merged");
        Assert.assertEquals(mergedQueys.get("NSR:Quay:11001b"), Sets.newHashSet("NSR:Quay:11004", "SKY:Quay:12348413"), "Quays should have been merged");
        Assert.assertEquals(mergedQueys.get("NSR:Quay:87131"), Sets.newHashSet("TRO:Quay:1903530601"), "Quays should have been merged");
        Assert.assertEquals(mergedQueys.get("NSR:Quay:87130"), Sets.newHashSet("TRO:Quay:19035306","TRO:Quay:1903530602"), "Quays should have been merged");


    }

    public String getQuayIdFromXdmItem(String xdmItem) {
        Pattern p = Pattern.compile("Quay:([0-9]+)");
        Matcher m = p.matcher(xdmItem);
        String group = null;
        while (m.find()) {
            group = m.group(1);
        }
        return group;
    }

    public String getStopPlaceIdFromXdmItem(String xdmItem) {
        Pattern  p = Pattern.compile("monomodalStopPlace:([0-9]+)");
        Matcher m = p.matcher(xdmItem);
        String group = null;
        while (m.find()) {
            group = m.group(1);
        }
        return group;
    }

    @Test
    public void testGetStopPlaceIdFromXdmItem() {

        String test = getStopPlaceIdFromXdmItem("FR::monomodalStopPlace:57130:FR1");
        Assert.assertEquals(test, "57130");

    }

    @Test
    public void testGetQuayIdFromXdmItem() {

        String test = getQuayIdFromXdmItem("id=\"FR::Quay:50095399:FR1\"");
        Assert.assertEquals(test, "50095399");

    }


    @Test
    public void testIDFM() throws Exception {
        //Config
        Processor proc = new Processor(false);
        File file = new File("src/test/resources/netex/getAll.xml");
        XdmNode document = proc.newDocumentBuilder().build(file);
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

            Pair<String, String> zderAndZdlr = new Pair<>(idZder, idZdlr);
            dataFromXml.put(idZdep, zderAndZdlr);
        }

        dataFromXml.forEach((zdep, zderAndZdlr) -> {
            System.out.print("Zdep: " + zdep+ " ");
            System.out.print("Zder: " + zderAndZdlr.first() + " ");
            System.out.println("Zdlr: " + zderAndZdlr.second() + " ");
        });
//        NetexXMLProcessingHelperFactory importer = new NetexXMLProcessingHelperFactory();
//        File file = new File("src/test/resources/netex/getAll.xml");
//        PublicationDeliveryStructure unmarshal = importer.unmarshal(file, new HashSet<>());
//        PublicationDeliveryStructure test = (PublicationDeliveryStructure) unmarshal;
//
//        Common_VersionFrameStructure value = test.getDataObjects().getCompositeFrameOrCommonFrame().get(0).getValue();
//
//        List<JAXBElement<? extends Common_VersionFrameStructure>> dataObjectFrames = test.getDataObjects().getCompositeFrameOrCommonFrame();
//        List<CompositeFrame> compositeFrames = NetexObjectUtil.getFrames(CompositeFrame.class, dataObjectFrames);
//        Optional<JAXBElement<? extends Common_VersionFrameStructure>> generalFrameArrets =
//                compositeFrames.get(0).getFrames().getCommonFrame().stream()
//                        .filter(frame -> "FR1:TypeOfFrame:NETEX_ARRET_IDF:".equals(frame.getValue().getTypeOfFrameRef().getRef()))
//                        .findAny();
//
//        List<SiteFrame> siteFrames = NetexObjectUtil.getFrames(SiteFrame.class, compositeFrames.get(0).getFrames().getCommonFrame());
//
//        if (generalFrameArrets.isPresent()) {
//            Common_VersionFrameStructure frameArrets = generalFrameArrets.get().getValue();
//        } else {
//            //TODO
//        }


    }



}
