package mobi.chouette.exchange.stopplace;

import com.google.common.collect.Sets;

import mobi.chouette.exchange.importer.updater.IdfmReflexParser;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.netexprofile.util.NetexObjectUtil;
import mobi.chouette.model.StopArea;
import net.sf.saxon.s9api.*;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.rutebanken.netex.model.*;
import org.testng.Assert;
import org.testng.annotations.Test;
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

    IdfmReflexParser idfmReflexParser;

    @Test
    public void testIDFM() throws Exception {
        //Config
        File file = new File("src/test/resources/netex/getAll.xml");
        HashMap<String, Pair<String, String>> stringPairHashMap = idfmReflexParser.parseReflexResult(new FileInputStream(file));

    }



}
