package mobi.chouette.exchange.stopplace;

import com.google.common.collect.Sets;

import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.model.StopArea;

import net.sf.saxon.s9api.XdmNode;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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



    @Test
    public void testIDFM() throws Exception {

        NetexXMLProcessingHelperFactory importer = new NetexXMLProcessingHelperFactory();
        File file = new File("src/test/resources/netex/IDFM_Reflex_all.xml");

        XdmNode dom = importer.parseFileToXdmNode(file, new HashSet<>());
        PublicationDeliveryStructure unmarshal = importer.unmarshal(file, new HashSet<>());

        System.out.println(unmarshal.getDataObjects());

    }

}
