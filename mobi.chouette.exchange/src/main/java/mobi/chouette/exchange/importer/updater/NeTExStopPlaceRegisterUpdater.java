package mobi.chouette.exchange.importer.updater;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ContenerChecker;
import mobi.chouette.common.Context;
import mobi.chouette.common.PropertyNames;
import mobi.chouette.exchange.importer.updater.netex.NavigationPathMapper;
import mobi.chouette.exchange.importer.updater.netex.StopAreaMapper;
import mobi.chouette.exchange.importer.updater.netex.StopPlaceMapper;
import mobi.chouette.model.Line;
import mobi.chouette.model.*;
import mobi.chouette.model.Route;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.rutebanken.netex.client.PublicationDeliveryClient;
import org.rutebanken.netex.client.TokenService;
import org.rutebanken.netex.model.*;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static mobi.chouette.common.PropertyNames.*;

@Log4j
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton(name = NeTExStopPlaceRegisterUpdater.BEAN_NAME)
public class NeTExStopPlaceRegisterUpdater {
    private static final String STOP_PLACE_REGISTER_MAP = "STOP_PLACE_REGISTER_MAP";

    private static final String VERSION = "1";

    public static final String IMPORTED_ID = "imported-id";

    public static final String MERGED_ID = "merged-id";

    public static final String BEAN_NAME = "NeTExStopPlaceRegisterUpdater";

    public static final String IMPORTED_ID_VALUE_SEPARATOR = ",";

    private PublicationDeliveryClient client;

    private final StopPlaceMapper stopPlaceMapper = new StopPlaceMapper();

    private final StopAreaMapper stopAreaMapper = new StopAreaMapper();

    private NavigationPathMapper navigationPathMapper = null;

    private static final ObjectFactory objectFactory = new ObjectFactory();

    private final Set<TransportModeNameEnum> busEnums = new HashSet<>(Arrays.asList(TransportModeNameEnum.Coach, TransportModeNameEnum.Bus));

    public NeTExStopPlaceRegisterUpdater(PublicationDeliveryClient client) throws DatatypeConfigurationException {
        this.client = client;
        navigationPathMapper = new NavigationPathMapper();
    }

    public NeTExStopPlaceRegisterUpdater() throws DatatypeConfigurationException {
        navigationPathMapper = new NavigationPathMapper();
    }


    @EJB
    private ContenerChecker contenerChecker;

    @PostConstruct
    public void postConstruct() {
        String url = getAndValidateProperty(PropertyNames.STOP_PLACE_REGISTER_URL);
        String clientId = getAndValidateProperty(KC_CLIENT_ID);
        String clientSecret = getAndValidateProperty(KC_CLIENT_SECRET);
        String realm = getAndValidateProperty(KC_CLIENT_REALM);
        String authServerUrl = getAndValidateProperty(KC_CLIENT_AUTH_URL);

        try {
            this.client = new PublicationDeliveryClient(url, false, new TokenService(clientId, clientSecret, realm, authServerUrl));
        } catch (JAXBException | SAXException | IOException e) {
            log.warn("Cannot initialize publication delivery client with URL '" + url + "'", e);
        }


    }

    public void update(Context context, Referential referential) throws JAXBException, DatatypeConfigurationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        if (client == null) {
            throw new RuntimeException("Looks like PublicationDeliveryClient is not set up correctly. Aborting.");
        }

        // Use a correlation ID that will be set as ID on the site frame sent to
        // the stop place register.
        // This correlation ID shall be defined in every log line related to
        // this publication delivery
        // to be able to trace logs both in chouette and the stop place
        // register.
        final String correlationId = UUID.randomUUID().toString();

        @SuppressWarnings("unchecked")
        Map<String, String> stopPlaceRegisterMap = (Map<String, String>) context.get(STOP_PLACE_REGISTER_MAP);
        if (stopPlaceRegisterMap == null) {
            stopPlaceRegisterMap = new HashMap<>();
            context.put(STOP_PLACE_REGISTER_MAP, stopPlaceRegisterMap);
        }

        final Map<String, String> m = stopPlaceRegisterMap;

        Predicate<StopArea> fullStopAreaNotCached = t -> {
            if (m.containsKey(t.getObjectId())) {
                for (StopArea child : t.getContainedStopAreas()) {
                    if (!m.containsKey(child.getObjectId())) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        };

        List<StopArea> boardingPositionsWithoutParents = referential.getStopAreas().values().stream()
                .filter(stopArea -> fullStopAreaNotCached.test(stopArea))
                .filter(stopArea -> stopArea.getAreaType() == ChouetteAreaEnum.BoardingPosition)
                .filter(stopArea -> stopArea.getParent() == null).filter(stopArea -> stopArea.getObjectId() != null)
                .collect(Collectors.toList());

        List<StopArea> createdParents = new ArrayList<StopArea>();

        for (StopArea bp : boardingPositionsWithoutParents) {
            StopArea csp = stopAreaMapper.mapCommercialStopPoint(referential, bp);
            createdParents.add(csp);
            log.info("created parent " + csp.getObjectId() + " for " + bp.getObjectId());
        }

        // Find and convert valid StopAreas
        List<StopPlace> stopPlaces = referential.getStopAreas().values().stream()
                .map(stopArea -> stopArea.getParent() == null ? stopArea : stopArea.getParent())
                .filter(fullStopAreaNotCached)
                .filter(stopArea -> stopArea.getObjectId() != null)
                .filter(stopArea -> stopArea.getAreaType() == ChouetteAreaEnum.CommercialStopPoint)
                .distinct()
                .peek(stopArea -> log.info(stopArea.getObjectId() + " name: " + stopArea.getName() + " correlationId: " + correlationId))
                .map(stopPlaceMapper::mapStopAreaToStopPlace)
                .map(stopArea -> stopPlaceMapper.addImportedIdInfo(stopArea, referential))
                .collect(Collectors.toList());

        SiteFrame siteFrame = new SiteFrame();
        siteFrame.setVersion(VERSION);

//		List<NavigationPath> navigationPaths = findAndMapConnectionLinks(referential, correlationId, siteFrame, m);

        List<StopPlace> stopPlacesToDelete = new ArrayList<>();

        if (!stopPlaces.isEmpty()) {

            // Only keep uniqueIds to avoid duplicate processing
            Set<String> uniqueIds = stopPlaces.stream().map(s -> s.getId()).collect(Collectors.toSet());
            stopPlaces = stopPlaces.stream().filter(s -> uniqueIds.remove(s.getId())).collect(Collectors.toList());

            // Find transport mode for stop place
            for (StopPlace stopPlace : stopPlaces) {

                String id = stopPlace.getId();
                StopArea stopArea = referential.getSharedStopAreas().get(stopPlace.getId());
                if (id.contains(ObjectIdTypes.STOPAREA_KEY)) {
                    // Only replace IDs if ID already contains Chouette ID key
                    // (StopArea)
                    stopPlaceMapper.replaceIdIfQuayOrStopPlace(stopPlace);
                }

                Quays_RelStructure quays = stopPlace.getQuays();
                if (quays != null && quays.getQuayRefOrQuay() != null) {
                    for (Object q : quays.getQuayRefOrQuay()) {
                        if (q instanceof Quay) {
                            Quay quay = (Quay) q;
                            String qId = quay.getId();
                            if (qId.contains(ObjectIdTypes.STOPAREA_KEY)) {
                                // Only replace IDs if ID already contains
                                // Chouette ID key (StopArea)
                                stopPlaceMapper.replaceIdIfQuayOrStopPlace(quay);
                            }
                        }
                    }
                }

                if (stopArea == null) {
                    log.error("Could not find StopArea for objectId="
                            + ToStringBuilder.reflectionToString(stopPlace)
                            + " correlationId: "
                            + correlationId);
                } else {
                    // Recursively find all transportModes
                    Set<TransportModeNameEnum> transportMode = NeTExStopPlaceUtil.findTransportModeForStopArea(new HashSet<>(), stopArea);
                    if (transportMode.size() > 1) {
                        if (busEnums.equals(transportMode)) {
                            stopPlaceMapper.mapTransportMode(stopPlace, TransportModeNameEnum.Bus);
                        } else {

                            log.info("Found more than one transport mode for StopArea with id "
                                    + stopPlace.getId()
                                    + ": "
                                    + ToStringBuilder.reflectionToString(transportMode.toArray(), ToStringStyle.SIMPLE_STYLE)
                                    + ", will use "
                                    + transportMode.iterator().next()
                                    + " correlationId: "
                                    + correlationId);
                            stopPlaceMapper.mapTransportMode(stopPlace, TransportModeNameEnum.Other);
                        }
                    } else if (transportMode.size() == 1) {
                        stopPlaceMapper.mapTransportMode(stopPlace, transportMode.iterator().next());
                    } else {
                        log.info("No transport modes found for StopArea with id "
                                + stopPlace.getId()
                                + " correlationId: "
                                + correlationId);
                        stopPlacesToDelete.add(stopPlace);
                    }
                }
            }

            // Do not add stop places with no transport mode as they belong to no route.
            stopPlaces.removeAll(stopPlacesToDelete);

            siteFrame.setStopPlaces(new StopPlacesInFrame_RelStructure().withStopPlace(stopPlaces));

            log.info("Create site frame with " + stopPlaces.size() + " stop places. correlationId: " + correlationId);
        }

        if (!stopPlaces.isEmpty()) {
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

            log.info("Got publication delivery structure back with "
                    + response.getDataObjects().getCompositeFrameOrCommonFrame().size()
                    + " composite frames or common frames correlationId: "
                    + correlationId);

            List<StopPlace> receivedStopPlaces = response.getDataObjects().getCompositeFrameOrCommonFrame().stream()
                    .filter(jaxbElement -> jaxbElement.getValue() instanceof SiteFrame)
                    .map(jaxbElement -> (SiteFrame) jaxbElement.getValue())
                    .filter(receivedSiteFrame -> receivedSiteFrame.getStopPlaces() != null)
                    .filter(receivedSiteFrame -> receivedSiteFrame.getStopPlaces().getStopPlace() != null)
                    .flatMap(receivedSiteFrame -> receivedSiteFrame.getStopPlaces().getStopPlace().stream())
                    .peek(stopPlace -> log.info("got stop place with ID "
                            + stopPlace.getId()
                            + " and name "
                            + stopPlace.getName()
                            + " back. correlationId: "
                            + correlationId))

                    .collect(Collectors.toList());

            log.info("Collected "
                    + receivedStopPlaces.size()
                    + " stop places from stop place register response. correlationId: "
                    + correlationId);

            AtomicInteger mappedStopPlacesCount = new AtomicInteger();
            receivedStopPlaces.forEach(e -> {
                stopAreaMapper.mapStopPlaceToStopArea(referential, e);
                mappedStopPlacesCount.incrementAndGet();
            });

            log.info("Mapped "
                    + mappedStopPlacesCount.get()
                    + " stop places into stop areas. correlationId: "
                    + correlationId);

            // Create map of existing object id -> new object id
            for (StopPlace newStopPlace : receivedStopPlaces) {
                KeyListStructure keyList = newStopPlace.getKeyList();
                addIdsToLookupMap(stopPlaceRegisterMap, keyList, newStopPlace.getId());

                Quays_RelStructure quays = newStopPlace.getQuays();
                if (quays != null && quays.getQuayRefOrQuay() != null) {
                    for (Object b : quays.getQuayRefOrQuay()) {
                        Quay q = (Quay) b;
                        KeyListStructure qKeyList = q.getKeyList();
                        addIdsToLookupMap(stopPlaceRegisterMap, qKeyList, q.getId());
                    }
                }
            }

            log.info("Map with objectId->newObjectId now contains "
                    + stopPlaceRegisterMap.keySet().size()
                    + " keys (objectIds) and "
                    + stopPlaceRegisterMap.values().size()
                    + " values (newObjectIds). correlationId: "
                    + correlationId);

            // Create map of existing object id -> new object id
            List<PathLink> receivedPathLinks = response.getDataObjects().getCompositeFrameOrCommonFrame().stream()
                    .filter(jaxbElement -> jaxbElement.getValue() instanceof SiteFrame)
                    .map(jaxbElement -> (SiteFrame) jaxbElement.getValue())
                    .filter(plStucture -> plStucture.getPathLinks() != null)
                    .filter(plStructure -> plStructure.getPathLinks() != null)
                    .filter(plStructure -> plStructure.getPathLinks().getPathLink() != null)
                    .flatMap(plStructure -> plStructure.getPathLinks().getPathLink().stream())
                    .peek(pl -> log
                            .info("got path link with ID " + pl.getId() + " back. correlationId: " + correlationId))
                    .collect(Collectors.toList());

            receivedPathLinks.forEach(e -> navigationPathMapper.mapPathLinkToConnectionLink(referential, e));

            for (PathLink pl : receivedPathLinks) {
                KeyListStructure keyList = pl.getKeyList();
                addIdsToLookupMap(stopPlaceRegisterMap, keyList, pl.getId());
            }

        }
        Set<String> discardedStopAreas = new HashSet<>();

        // Update each stopPoint
        for (Line line : referential.getLines().values()) {
            for (Route r : line.getRoutes()) {
                for (StopPoint sp : r.getStopPoints()) {
                    updateStopAreaForStopPoint(correlationId, stopPlaceRegisterMap, referential, discardedStopAreas, sp);
                }
            }
        }
        for (RouteSection rs : referential.getRouteSections().values()) {
//            updateStopArea(correlationId, stopPlaceRegisterMap, referential, discardedStopAreas, rs, "arrival");
            updateRouteSectionArrival(correlationId, stopPlaceRegisterMap, referential, discardedStopAreas, rs);
//            updateStopArea(correlationId, stopPlaceRegisterMap, referential, discardedStopAreas, rs, "departure");
            updateRouteSectionDeparture(correlationId, stopPlaceRegisterMap, referential, discardedStopAreas, rs);
        }
        // TODO update stoparea in connectionlinks and accesslinks (check uml
        // diagram for usage of stoparea

        // TODO? remove obsolete connectionLinks?
//		List<ConnectionLink> removedCollectionLinks = referential.getSharedConnectionLinks().values().stream()
//				.filter(e -> m.containsKey(e.getObjectId())).collect(Collectors.toList());
//
//		removedCollectionLinks.stream()
//				.peek(e -> log.info(
//						"Removing old connectionLink with id " + e.getObjectId() + ". correlationId: " + correlationId))
//				.map(e -> referential.getSharedConnectionLinks().remove(e.getObjectId())).collect(Collectors.toList());

        // Clean referential from old garbage stop areas
        for (String obsoleteObjectId : discardedStopAreas) {
            referential.getStopAreas().remove(obsoleteObjectId);
        }

        for (

                StopArea sa : createdParents) {
            referential.getStopAreas().remove(sa.getObjectId());
        }

    }


    private List<NavigationPath> findAndMapConnectionLinks(Referential referential, String correlationId, SiteFrame siteFrame, Map<String, String> m) {
        referential.getSharedConnectionLinks().clear(); // Nuke connection links
        // fully to avoid old
        // stopareas being
        // persisted

        return referential.getSharedConnectionLinks().values().stream()
                .filter(link -> !m.containsKey(link.getObjectId()))
                .peek(link -> log.debug(link.getObjectId() + " correlationId:" + correlationId))
                .map(link -> navigationPathMapper.mapConnectionLinkToNavigationPath(siteFrame,
                        link))
                .collect(Collectors.toList());
    }

    private void updateStopArea(String correlationId, Map<String, String> map, Referential referential,
                                Set<String> discardedStopAreas, NeptuneIdentifiedObject sp, String name)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        StopArea stopArea = (StopArea) PropertyUtils.getProperty(sp, name);
        String currentObjectId = stopArea.getObjectId();
        String newObjectId = map.get(currentObjectId);

        if (newObjectId != null) {
            if (!currentObjectId.equals(newObjectId)) {
                StopArea newStopArea = referential.getSharedStopAreas().get(newObjectId);
                if (newStopArea != null) {
                    PropertyUtils.setProperty(sp, name, newStopArea);
                    discardedStopAreas.add(currentObjectId);
                } else {

                    log.error("About to replace StopArea with id "
                            + currentObjectId
                            + " with "
                            + newObjectId
                            + ", but newStopArea does not exist in referential! correlationId: "
                            + correlationId);
                }
            }
        } else {
            log.warn("Could not find mapped object for "
                    + sp.getClass().getSimpleName()
                    + "/"
                    + name
                    + " "
                    + currentObjectId
                    + " "
                    + stopArea.getName()
                    + " correlationId: "
                    + correlationId);
        }
    }


    private void updateStopAreaForStopPoint(String correlationId, Map<String, String> map, Referential referential, Set<String> discardedStopAreas, StopPoint sp) {
        ScheduledStopPoint scheduledStopPoint = sp.getScheduledStopPoint();
        updateScheduledStopPointParentStopArea(correlationId, map, referential, discardedStopAreas, scheduledStopPoint, sp.getObjectId());

    }

    private void updateRouteSectionArrival(String correlationId, Map<String, String> map, Referential referential, Set<String> discardedStopAreas, RouteSection rs) {
        ScheduledStopPoint scheduledStopPoint = rs.getToScheduledStopPoint();
        updateScheduledStopPointParentStopArea(correlationId, map, referential, discardedStopAreas, scheduledStopPoint, rs.getObjectId());
    }

    private void updateRouteSectionDeparture(String correlationId, Map<String, String> map, Referential referential, Set<String> discardedStopAreas, RouteSection rs) {
        ScheduledStopPoint scheduledStopPoint = rs.getFromScheduledStopPoint();
        updateScheduledStopPointParentStopArea(correlationId, map, referential, discardedStopAreas, scheduledStopPoint, rs.getObjectId());
    }

    private void updateScheduledStopPointParentStopArea(String correlationId, Map<String, String> map, Referential referential, Set<String> discardedStopAreas, ScheduledStopPoint scheduledStopPoint, String objectId) {
        if (scheduledStopPoint != null) {
            StopArea stopArea = scheduledStopPoint.getContainedInStopAreaRef().getObject();
            String currentObjectId = stopArea.getObjectId();
            String newObjectId = map.get(currentObjectId);

            if (newObjectId != null) {
                if (!currentObjectId.equals(newObjectId)) {
                    StopArea newStopArea = referential.getSharedStopAreas().get(newObjectId);
                    if (newStopArea != null) {
                        scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference<>(newStopArea));
                        discardedStopAreas.add(currentObjectId);
                    } else {

                        log.error("About to replace StopArea with id "
                                + currentObjectId
                                + " with "
                                + newObjectId
                                + ", but newStopArea does not exist in referential! correlationId: "
                                + correlationId);
                    }
                }
            } else {
                log.warn("Could not find mapped object for " + currentObjectId + " / " + stopArea.getName() + " - correlation id : " + correlationId);
            }
        } else {
            log.warn("Could not find mapped object for " + objectId + " - correlation id : " + correlationId);
        }
    }


    private void addIdsToLookupMap(Map<String, String> map, KeyListStructure keyList, String newStopPlaceId) {
        // Add current id to map as well to handle if we send correct id's in and receive the same back
        map.put(newStopPlaceId, newStopPlaceId);

        if (keyList != null && keyList.getKeyValue() != null) {
            List<KeyValueStructure> keyValue = keyList.getKeyValue();

            for (KeyValueStructure s : keyValue) {
                if (s != null && IMPORTED_ID.equals(s.getKey())) {
                    // Split value
                    String[] existingIds = StringUtils.split(s.getValue(), IMPORTED_ID_VALUE_SEPARATOR);
                    for (String id : existingIds) {
                        map.put(id, newStopPlaceId);
                    }
                }
                if (s != null && MERGED_ID.equals(s.getKey())) {
                    // Split value
                    String[] existingIds = StringUtils.split(s.getValue(), IMPORTED_ID_VALUE_SEPARATOR);
                    for (String id : existingIds) {
                        map.put(id, newStopPlaceId);
                    }
                }
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
