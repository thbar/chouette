package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.*;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.LongLatTypeEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.client.PublicationDeliveryClient;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.validation.NeTExValidator;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

public class NeTExStopPlaceRegisterUpdaterTest {

    @Test
    public void convertStopAreaAndConnectionLink() throws Exception {

        Referential referential = new Referential();

        StopArea stopAreaCommercial = ObjectFactory.getStopArea(referential, "AKT:StopArea:1");
        stopAreaCommercial.setName("Nesbru");
        stopAreaCommercial.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
        stopAreaCommercial.setLatitude(new BigDecimal(59.9202707));
        stopAreaCommercial.setLongitude(new BigDecimal(10.7913503));
        stopAreaCommercial.setLongLatType(LongLatTypeEnum.WGS84);

        StopArea stopAreaBoarding2 = ObjectFactory.getStopArea(referential, "AKT:StopArea:2");
        stopAreaBoarding2.setAreaType(ChouetteAreaEnum.BoardingPosition);
        stopAreaBoarding2.setLatitude(new BigDecimal(59.9202707));
        stopAreaBoarding2.setLongitude(new BigDecimal(10.7913503));
        stopAreaBoarding2.setLongLatType(LongLatTypeEnum.WGS84);
        stopAreaBoarding2.setParent(stopAreaCommercial);

        StopArea stopAreaBoarding3 = ObjectFactory.getStopArea(referential, "AKT:StopArea:3");
        stopAreaBoarding3.setAreaType(ChouetteAreaEnum.BoardingPosition);
        stopAreaBoarding3.setLatitude(new BigDecimal(59.9302707));
        stopAreaBoarding3.setLongitude(new BigDecimal(10.7813503));
        stopAreaBoarding3.setLongLatType(LongLatTypeEnum.WGS84);
        stopAreaBoarding3.setParent(stopAreaCommercial);

        Line line = ObjectFactory.getLine(referential, "AKT:Line:1");
        Route route = ObjectFactory.getRoute(referential, "AKT:Route:1");
        route.setLine(line);

        StopPoint sp2 = ObjectFactory.getStopPoint(referential, "AKT:StopPoint:2");
        ScheduledStopPoint ssp2 = ObjectFactory.getScheduledStopPoint(referential, "AKT:ScheduledStopPoint:2");
        ssp2.setContainedInStopAreaRef(new SimpleObjectReference(stopAreaBoarding2));
        sp2.setScheduledStopPoint(ssp2);

        StopPoint sp3 = ObjectFactory.getStopPoint(referential, "AKT:StopPoint:3");
        ScheduledStopPoint ssp3 = ObjectFactory.getScheduledStopPoint(referential, "AKT:ScheduledStopPoint:3");
        ssp3.setContainedInStopAreaRef(new SimpleObjectReference(stopAreaBoarding3));
        sp3.setScheduledStopPoint(ssp3);

        route.getStopPoints().add(sp2);
        route.getStopPoints().add(sp3);


        Context context = new Context();

        // Build response
        NeTExStopPlaceRegisterUpdater neTExStopPlaceRegisterUpdater = new NeTExStopPlaceRegisterUpdater(createMockedPublicationDeliveryClient(stopAreaCommercial));

        // Call update
        neTExStopPlaceRegisterUpdater.update(context, referential);

        // Assert stopPoints changed
        assertEquals("NHR:StopArea:2",sp2.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId());
        assertEquals("NHR:StopArea:3", sp3.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId());

    }

    /**
     * Validate PublicationDeliveryStructure.
     */
    private void validate(PublicationDeliveryStructure publicationDeliveryStructure) throws JAXBException, IOException {
        JAXBContext jaxbContext = JAXBContext.newInstance(PublicationDeliveryStructure.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        JAXBSource jaxbSource = new JAXBSource(jaxbContext, new org.rutebanken.netex.model.ObjectFactory().createPublicationDelivery(publicationDeliveryStructure));
        try {
            NeTExValidator neTExValidator = new NeTExValidator();
            neTExValidator.getSchema().newValidator().validate(jaxbSource);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private PublicationDeliveryClient createMockedPublicationDeliveryClient(StopArea stopArea) throws JAXBException, IOException, SAXException {
        return new PublicationDeliveryClient("", null) {
            @Override
            public PublicationDeliveryStructure sendPublicationDelivery(
                    PublicationDeliveryStructure publicationDelivery) throws JAXBException, IOException {

                validate(publicationDelivery);

                Assert.assertEquals(1, ((SiteFrame) publicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame().get(0).getValue()).getStopPlaces().getStopPlace().size(), "StopPlaces not unique");

                SimplePoint_VersionStructure centroid = new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure().withLatitude(stopArea.getLatitude())
                                .withLongitude(stopArea.getLongitude()));

                StopPlace stopPlace = new StopPlace();
                stopPlace.setId("NHR:StopArea:1");
                stopPlace.setCentroid(centroid);
                stopPlace.setName(new MultilingualString().withValue("StopPlaceName"));
                stopPlace.setKeyList(createKeyListStructure("AKT:StopArea:1"));

                Quay q2 = new Quay();
                q2.setId("NHR:StopArea:2");
                q2.setKeyList(createKeyListStructure("AKT:StopArea:2"));
                q2.setName(new MultilingualString().withValue("QuayName"));
                q2.setCentroid(centroid);

                Quay q3 = new Quay();
                q3.setId("NHR:StopArea:3");
                q3.setKeyList(createKeyListStructure("AKT:StopArea:3"));
                q3.setName(new MultilingualString().withValue("QuayName"));
                q3.setCentroid(centroid);

                Quays_RelStructure quays = new Quays_RelStructure();
                quays.getQuayRefOrQuay().add(q2);
                quays.getQuayRefOrQuay().add(q3);
                stopPlace.setQuays(quays);

                List<StopPlace> stopPlaces = new ArrayList<>();
                stopPlaces.add(stopPlace);

                SiteFrame siteFrame = new SiteFrame();
                siteFrame.setStopPlaces(new StopPlacesInFrame_RelStructure().withStopPlace(stopPlaces));

                org.rutebanken.netex.model.ObjectFactory objectFactory = new org.rutebanken.netex.model.ObjectFactory();
                JAXBElement<SiteFrame> jaxSiteFrame = objectFactory.createSiteFrame(siteFrame);

                PublicationDeliveryStructure respoonse = new PublicationDeliveryStructure()
                        .withDescription(
                                new MultilingualString().withValue("Publication delivery from chouette")
                                        .withLang("fr").withTextIdType(""))
                        .withPublicationTimestamp(LocalDateTime.now()).withParticipantRef("participantRef")
                        .withDataObjects(new PublicationDeliveryStructure.DataObjects()
                                .withCompositeFrameOrCommonFrame(Arrays.asList(jaxSiteFrame)));
                return respoonse;

            }

            protected KeyListStructure createKeyListStructure(String value) {
                KeyListStructure kl = new KeyListStructure();
                KeyValueStructure kv = new KeyValueStructure();
                kv.setKey(NeTExStopPlaceRegisterUpdater.IMPORTED_ID);
                kv.setValue(value);
                kl.getKeyValue().add(kv);
                return kl;
            }
        };
    }
}