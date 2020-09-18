package mobi.chouette.exchange.importer.updater;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ContenerChecker;
import mobi.chouette.common.Context;
import mobi.chouette.common.PropertyNames;
import mobi.chouette.exchange.importer.updater.netex.StopPlaceMapper;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang.StringUtils;
import org.rutebanken.netex.client.PublicationDeliveryClient;
import org.rutebanken.netex.client.TokenService;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopPlacesInFrame_RelStructure;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static mobi.chouette.common.PropertyNames.KC_CLIENT_AUTH_URL;
import static mobi.chouette.common.PropertyNames.KC_CLIENT_ID;
import static mobi.chouette.common.PropertyNames.KC_CLIENT_REALM;
import static mobi.chouette.common.PropertyNames.KC_CLIENT_SECRET;
import static mobi.chouette.exchange.importer.updater.NeTExStopPlaceUtil.findTransportModeForStopArea;

@Log4j
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton(name = NeTExIdfmStopPlaceRegisterUpdater.BEAN_NAME)
public class NeTExIdfmStopPlaceRegisterUpdater {
    private static final String STOP_PLACE_REGISTER_MAP = "STOP_PLACE_REGISTER_MAP";

    private static final String VERSION = "1";

    public static final String IMPORTED_ID = "imported-id";

    public static final String ZDEP = "zdep";

    public static final String BEAN_NAME = "NeTExIdfmStopPlaceRegisterUpdater";

    private PublicationDeliveryClient client;

    private final StopPlaceMapper stopPlaceMapper = new StopPlaceMapper();

    private static final ObjectFactory objectFactory = new ObjectFactory();

    private final Set<TransportModeNameEnum> busEnums = new HashSet<>(Arrays.asList(TransportModeNameEnum.Coach, TransportModeNameEnum.Bus));

    public NeTExIdfmStopPlaceRegisterUpdater(PublicationDeliveryClient client) {
        this.client = client;
    }

    public NeTExIdfmStopPlaceRegisterUpdater() {
    }

    @EJB
    private ContenerChecker contenerChecker;

    @PostConstruct
    public void postConstruct() {
        initializeClient();
    }

    private void initializeClient(){
        initializeClient(null);
    }

    private void initializeClient(String ref){
        String url = getAndValidateProperty(PropertyNames.STOP_PLACE_REGISTER_URL);
        url = url.replace("ID_MATCH", "MATCH");
        if(!StringUtils.isEmpty(ref)) {
            if(url.contains("?"))
                url = url + "&providerCode=" + ref;
            else
                url = url + "?providerCode=" + ref;
        }
        String clientId = getAndValidateProperty(KC_CLIENT_ID);
        String clientSecret = getAndValidateProperty(KC_CLIENT_SECRET);
        String realm = getAndValidateProperty(KC_CLIENT_REALM);
        String authServerUrl = getAndValidateProperty(KC_CLIENT_AUTH_URL);

        /**
         * WORKAROUND
         */
//        url = "http://kong:8000/api/stop_places/1.0/netex";
//        clientId = "chouette";
//        clientSecret = "314a5096-ed83-45ae-8dd6-904639a68806";
//        realm = "Naq";
//        authServerUrl = "https://auth-rmr.nouvelle-aquitaine.pro/auth/";

        try {
            this.client = new PublicationDeliveryClient(url, false, new TokenService(clientId, clientSecret, realm, authServerUrl));
        } catch (JAXBException | SAXException | IOException e) {
            log.warn("Cannot initialize publication delivery client with URL '" + url + "'", e);
        }
    }

    public void update(Context context, Referential referential, List<StopArea> stopAreas) throws JAXBException, DatatypeConfigurationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        String ref = (String) context.get("ref");
        initializeClient(ref);

        if (client == null) {
            throw new RuntimeException("Looks like PublicationDeliveryClient is not set up correctly. Aborting.");
        }

        final String correlationId = UUID.randomUUID().toString();

        // Find and convert valid StopAreas
        List<StopPlace> stopPlaces = null;
        for(StopArea area : stopAreas){
            StopArea finalStopArea;
            if(area.getParent() != null)
                finalStopArea = area.getParent();
            else
                finalStopArea = area;

            if(finalStopArea.getObjectId() == null) continue;
            if(finalStopArea.getAreaType() != ChouetteAreaEnum.CommercialStopPoint) continue;

            List<StopPlace> stopPlaceList =  Arrays.asList(finalStopArea).stream()
                    .peek(stopArea -> log.info(stopArea.getObjectId() + " name: " + stopArea.getName() + " correlationId: " + correlationId))
                    .map(stopPlaceMapper::mapStopAreaToStopPlace)
                    //.map(stopArea -> stopPlaceMapper.addImportedIdfmInfo(stopArea, referential, stopAreas))
                    .collect(Collectors.toList());
            /*
            SCH
             */
            List<StopPoint> stopPoints = area.getContainedScheduledStopPoints().stream().map(ScheduledStopPoint::getStopPoints).flatMap(List::stream).collect(Collectors.toList());
            Set<TransportModeNameEnum> transportMode = findTransportModeForStopArea(new HashSet<>(), area);

            if (transportMode.size() > 1) {
                if (busEnums.equals(transportMode)) {
                    stopPlaceMapper.mapTransportMode(stopPlaceList.get(0), TransportModeNameEnum.Bus);
                } else {
                    stopPlaceMapper.mapTransportMode(stopPlaceList.get(0), TransportModeNameEnum.Other);
                }
            } else if (transportMode.size() == 1) {
                stopPlaceMapper.mapTransportMode(stopPlaceList.get(0), transportMode.iterator().next());
            }
            /*
            SCH
             */

            if(stopPlaces == null || stopPlaces.size() == 0)
                stopPlaces = stopPlaceList;
            else
                stopPlaces.addAll(stopPlaceList);
        }

        SiteFrame siteFrame = new SiteFrame();
        siteFrame.setVersion(VERSION);

        if (stopPlaces != null && !stopPlaces.isEmpty()) {

            // Only keep uniqueIds to avoid duplicate processing
            Set<String> uniqueIds = stopPlaces.stream().map(s -> s.getId()).collect(Collectors.toSet());
            stopPlaces = stopPlaces.stream().filter(s -> uniqueIds.remove(s.getId())).collect(Collectors.toList());

//            // Find transport mode for stop place
//            for (StopPlace stopPlace : stopPlaces) {
//                StopArea stopArea = referential.getSharedStopAreas().get(stopPlace.getId());
//                if (stopArea != null) {
//
//                    // Recursively find all transportModes
//                    Set<TransportModeNameEnum> transportMode = NeTExStopPlaceUtil.findTransportModeForStopArea(new HashSet<>(), stopArea);
//                    if (transportMode.size() > 1) {
//                        if (busEnums.equals(transportMode)) {
//                            stopPlaceMapper.mapTransportMode(stopPlace, TransportModeNameEnum.Bus);
//                        } else {
//                            stopPlaceMapper.mapTransportMode(stopPlace, TransportModeNameEnum.Other);
//                        }
//                    } else if (transportMode.size() == 1) {
//                        stopPlaceMapper.mapTransportMode(stopPlace, transportMode.iterator().next());
//                    }
//                }
//            }

            siteFrame.setStopPlaces(new StopPlacesInFrame_RelStructure().withStopPlace(stopPlaces));
        }

        if (stopPlaces != null && !stopPlaces.isEmpty()) {
            siteFrame.setCreated(LocalDateTime.now());
            siteFrame.setId(correlationId);

            JAXBElement<SiteFrame> jaxSiteFrame = objectFactory.createSiteFrame(siteFrame);

            PublicationDeliveryStructure publicationDelivery = new PublicationDeliveryStructure()
                    .withDescription(new MultilingualString().withValue("Publication delivery from chouette")
                            .withLang("fr").withTextIdType(""))
                    .withPublicationTimestamp(LocalDateTime.now()).withParticipantRef("participantRef")
                    .withDataObjects(new PublicationDeliveryStructure.DataObjects()
                            .withCompositeFrameOrCommonFrame(Arrays.asList(jaxSiteFrame)));

            PublicationDeliveryStructure response;
            try {
                response = client.sendPublicationDelivery(publicationDelivery);
            } catch (JAXBException | IOException | SAXException e) {
                throw new RuntimeException("Got exception while sending publication delivery with "
                        + stopPlaces.size()
                        + " stop places to stop place register. correlationId: "
                        + correlationId, e);
            }

            if (response.getDataObjects() == null) {
                throw new RuntimeException("The response dataObjects is null for received publication delivery. Nothing to do here. "
                        + correlationId);

            } else if (response.getDataObjects().getCompositeFrameOrCommonFrame() == null) {
                throw new RuntimeException("Composite frame or common frame is null for received publication delivery. " + correlationId);
            }
        }
    }

    private String getAndValidateProperty(String propertyName) {
        String urlPropertyKey = contenerChecker.getContext() + propertyName;
        String propertyValue = System.getProperty(urlPropertyKey);
        if (propertyValue == null) {
            log.warn("Cannot read property " + urlPropertyKey + ". Will not update stop place registry.");
            this.client = null;
        }
        return propertyValue;
    }


}
