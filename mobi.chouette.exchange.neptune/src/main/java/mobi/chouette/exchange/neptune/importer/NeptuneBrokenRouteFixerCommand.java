package mobi.chouette.exchange.neptune.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.neptune.Constant;
import mobi.chouette.exchange.neptune.jaxb.JaxbNeptuneFileConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.trident.schema.trident.ChouettePTNetworkType.ChouetteLineDescription.ChouetteRoute;
import mobi.chouette.model.util.Referential;
import org.trident.schema.trident.ChouettePTNetworkType;
import org.trident.schema.trident.JourneyPatternType;
import org.trident.schema.trident.PTLinkType;
import org.trident.schema.trident.RouteExtension;
import org.trident.schema.trident.VehicleJourneyType;

import javax.naming.InitialContext;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j
public class NeptuneBrokenRouteFixerCommand implements Command, Constant {

    public static final String COMMAND = "NeptuneBrokenRouteFixerCommand";
    private Map<String,PTLinkType> ptLinkTypeMap = new HashMap<>();
    private Map<String, JourneyPatternType> journeyPatternTypeMap = new HashMap<>();
    private Map<String, ChouetteRoute> fixedRouteMap = new HashMap<>();
    private Map<String, List<String>> startingPtLinksMap = new HashMap<>();


    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;


        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            context.put(REFERENTIAL, new Referential());
            JobData jobData = (JobData) context.get(JOB_DATA);
            Path path = Paths.get(jobData.getPathName(), INPUT);

            List<Path> filesToProcess = FileUtil.listFiles(path, "*.xml", "*metadata*");
            filesToProcess.forEach(this::fixBrokenRouteOnFile);

            result = SUCCESS;

        } catch (Exception e) {
            log.error(e, e);
            result = ERROR;
            throw e;
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }
        return result;
    }


    /**
     * Fix all broken routes in the file passed as parameter
     * @param path
     *          path to the file that contains broken routes
     */
    private void fixBrokenRouteOnFile(Path path){
        log.info("Processing file:"+path.toAbsolutePath());
        try {
            JaxbNeptuneFileConverter converter = JaxbNeptuneFileConverter.getInstance();
            Optional<ChouettePTNetworkType> chouetteNetworkOpt = converter.read(path);

            if (!chouetteNetworkOpt.isPresent()){
                log.error("Unable to read unmarshalled file");
                return ;
            }

            ChouettePTNetworkType chouetteNetwork = chouetteNetworkOpt.get();

            List<ChouetteRoute> brokenRoutes = getBrokenRoutes(chouetteNetwork);

            if(brokenRoutes.isEmpty()){
                log.info("No broken route detected on network:"+chouetteNetwork.getPTNetwork().getObjectId());
            }else{
                log.warn(brokenRoutes.size()+" broken routes detected");
                brokenRoutes.forEach(brokenRoute->fixBrokenRoute(chouetteNetwork,brokenRoute));
                fixVehicleJourneyRouteIds(chouetteNetwork);
                cleanWaybackRoutes(chouetteNetwork);

                File originalFile = new File(path.toAbsolutePath().toString());
                File directory = originalFile.getParentFile();
                String originalFileName = FilenameUtils.removeExtension(originalFile.getName());
                originalFile.delete();
                File outPutFile = new File(directory,originalFile.getName());
                converter.write(chouetteNetwork,outPutFile);
                log.info(outPutFile.getName()+" created with fixed routes");



                //a backup copy of the generated file is stored in the job's root directory
                File jobRootDirectory = directory.getParentFile();
                String copyName = originalFileName+"-brFix.xml";
                File backupCopy = new File(jobRootDirectory,copyName);
                FileUtils.copyFile(outPutFile,backupCopy);

            }

        } catch (Exception e) {
            log.error("Error while processing file ");
            log.error(e);
        }
    }

    /***
     * Delete waybackRouteIds that refers to deleted routes
     * @param chouetteNetwork
     *      The network on which wayback routes must be checked
     */
    private void cleanWaybackRoutes(ChouettePTNetworkType chouetteNetwork){

        ChouettePTNetworkType.ChouetteLineDescription description = chouetteNetwork.getChouetteLineDescription();
        List<String> availableRouteIds = description.getChouetteRoute()
                                                    .stream()
                                                    .map(ChouetteRoute::getObjectId)
                                                    .collect(Collectors.toList());

        description.getChouetteRoute().stream()
                                      .filter(route->!availableRouteIds.contains(route.getWayBackRouteId()))
                                      .forEach(route-> route.setWayBackRouteId(null));

    }


    /**
     * Fix routeIds stored in vehicleJourney objects.
     * (To fix route, we need to create news routes, with new Ids. Once all new routes have been created, we need to update all vehicle Journeys and update their routeId)
     * @param chouetteNetwork
     *       The network root object that contains all obejcts of the line
     */
    private void fixVehicleJourneyRouteIds(ChouettePTNetworkType chouetteNetwork){
        chouetteNetwork.getChouetteLineDescription()
                .getVehicleJourney()
                .stream()
                .forEach(this::fixVehicleJourneyRouteId);

    }

    /**
     * Fix routeIds stored in vehicleJourney object passed as parameter.
     * (Reads the current routeId of the object and checks if it is equal to the routeId specified in the journeyPattern.
     * If not, it is updated)
     * @param vehicleJourney
     *      vehicleJourney on which routeId must be checked(and updated if needed)
     */
    private void fixVehicleJourneyRouteId(VehicleJourneyType vehicleJourney){
        String journeyPatternId = vehicleJourney.getJourneyPatternId();
        String journeyPatternRouteId = journeyPatternTypeMap.get(journeyPatternId).getRouteId();

        if (!journeyPatternRouteId.equals(vehicleJourney.getRouteId())){
            vehicleJourney.setRouteId(journeyPatternRouteId);
        }
    }


    /**
     * Fix all breaks existing in the ChouetteRoute passed as parameter.
     * (a break is found between 2 ptLinks when the end point of a ptLink does not match with the start point of the next ptLink)
     * Each time a break is found, a new route is created to separate into 2 different routes.
     * JourneyPatterns are then updated to use new routeIds.
     *
     * @param chouetteNetwork
     *         root object that contains all data of the line
     * @param chouetteBrokenRoute
     *        route object that contains breaks to be fixed
     */
    private void fixBrokenRoute(ChouettePTNetworkType chouetteNetwork, ChouetteRoute chouetteBrokenRoute){

        String brokenRouteId = chouetteBrokenRoute.getObjectId();
        log.info("Start fixing broken route:"+brokenRouteId);

        //Build fixed routes
        List<ChouetteRoute> rebuiltRoutes = buildNewRoutesFromBrokenRoute(chouetteBrokenRoute);

        log.info(rebuiltRoutes.size()+" new routes created");

        //Remove the old broken route
        chouetteNetwork.getChouetteLineDescription().getChouetteRoute().remove(chouetteBrokenRoute);
        chouetteNetwork.getChouetteLineDescription().getLine().getRouteId().removeIf(routeId->routeId.equals(brokenRouteId));

        //Add the new fixed routes
        chouetteNetwork.getChouetteLineDescription().getChouetteRoute().addAll(rebuiltRoutes);

        Set<String> rebuiltRoutesIds = rebuiltRoutes.stream()
                .map(ChouetteRoute::getObjectId)
                .collect(Collectors.toSet());

        chouetteNetwork.getChouetteLineDescription().getLine().getRouteId().addAll(rebuiltRoutesIds);

        //After creating new routes, all affected JourneyPatterns must be updated
        reAttributeJourneyPatterns(chouetteNetwork,rebuiltRoutes);
    }


    /***
     * Update all journeyPatterns's routeIds, after all routes are fixed.
     * (To fix routes, we creates new routes with new ID's. Journey Pattern must be updated to use these new ID's)
     *
     * @param chouetteNetwork
     *          root object that contains all data of the line
     * @param rebuiltRoutes
     *          list of all created routes. (only journeyPatterns used in new routes must be updated)
     */
    private void reAttributeJourneyPatterns(ChouettePTNetworkType chouetteNetwork, List<ChouetteRoute> rebuiltRoutes){
        for (ChouetteRoute route : rebuiltRoutes){
            List<String> routeJourneyPatterns = route.getJourneyPatternId();
            chouetteNetwork.getChouetteLineDescription().getJourneyPattern()
                    .stream()
                    .filter(journeyPattern->routeJourneyPatterns.contains(journeyPattern.getObjectId()))
                    .forEach(journeyPattern->journeyPattern.setRouteId(route.getObjectId()));

        }
    }


    /***
     * Create new fixed routes from a broken route.
     * (each time a break is found in the route between 2 ptLinks, a new route is created)
     * @param chouetteBrokenRoute
     *          the route containing breaks to fix
     * @return
     *          list of all newly created routes, without breaks
     */
    private List<ChouetteRoute> buildNewRoutesFromBrokenRoute(ChouetteRoute chouetteBrokenRoute){

        List<ChouetteRoute> rebuiltRoutes = new ArrayList<>();
        List<String> startingPtLinks = startingPtLinksMap.get(chouetteBrokenRoute.getObjectId());

        if (startingPtLinks.size() == 0){
            log.error("Error processing route :" + chouetteBrokenRoute.getObjectId());
            log.error("starting ptLinks have not been initialized");
            throw new RuntimeException("Error processing route :" + chouetteBrokenRoute.getObjectId());
        }

        int sectionNumber = 1;

        // startingPtLinks is feeded before in the process (in isBrokenRouteExisting function).
        // it contains all ptLinks that will start a new route
        // e.g: if there are 2 breaks in the route, it will contain 3 starting points: start of the current Route, start of segment 2 and start of segment 3
        for (String startingPtLink : startingPtLinks){
            rebuiltRoutes.add(buildNewRouteFromPtLink(chouetteBrokenRoute,sectionNumber,startingPtLink));
            sectionNumber++;
        }
        return rebuiltRoutes;
    }

    /**
     * Build a new route that starts on startPtLinkId and ends on the first break found
     * @param chouetteBrokenRoute
     *          whole route that contains breaks to be fixed
     * @param sectionNumber
     *          suffix that will be added to create a new ID
     * @param startPtLinkId
     *          ptLink that will start the new route
     * @return
     *          a new route that starts on startPtLinkId and ends on the first break found
     */
    private ChouetteRoute buildNewRouteFromPtLink(ChouetteRoute chouetteBrokenRoute,int sectionNumber, String startPtLinkId){
        String originalRouteId = chouetteBrokenRoute.getObjectId();

        ChouetteRoute chouetteRoute = createRouteWithGeneralInfo(originalRouteId+"-"+sectionNumber,chouetteBrokenRoute.getObjectVersion(),
                chouetteBrokenRoute.getCreationTime(),chouetteBrokenRoute.getName(), chouetteBrokenRoute.getRouteExtension());


        String lastEndPoint = null;
        String newRouteStartPoint = ptLinkTypeMap.get(startPtLinkId).getStartOfLink();

        for (String ptLinkId : chouetteBrokenRoute.getPtLinkId()){

            if (lastEndPoint == null && ptLinkId != startPtLinkId){
                //Start ptlink has not been reached yet. We continue until the start prLink has been reached
                continue;
            }

            if (lastEndPoint != null && !lastEndPoint.equals(ptLinkTypeMap.get(ptLinkId).getStartOfLink())){
                //The current start point of the segment does not match the last end point. There is a broken route. The current ptLink is not added
                break;
            }
            //the current ptLink is part of the route created. It is added
            chouetteRoute.getPtLinkId().add(ptLinkId);
            lastEndPoint = ptLinkTypeMap.get(ptLinkId).getEndOfLink();
        }

        chouetteRoute.getJourneyPatternId().addAll(getJourneyPatterns(originalRouteId,newRouteStartPoint,lastEndPoint));
        fixedRouteMap.put(chouetteRoute.getObjectId(),chouetteRoute);
        return chouetteRoute;
    }

    /**
     * Get all the journey patterns relative to routeId, starting on startPoint and ending on endPoint
     * @param routeId
     *          route on which journey patterns must be recovered
     * @param startPoint
     *          start point of the journey pattern
     * @param endPoint
     *          end point of the pattern
     * @return
     */
    private List<String> getJourneyPatterns(String routeId, String startPoint,String endPoint){
        return journeyPatternTypeMap.entrySet().stream()
                .filter(entry-> entry.getValue().getRouteId().equals(routeId) &&
                        entry.getValue().getStopPointList().get(0).equals(startPoint) &&
                        entry.getValue().getStopPointList().get(entry.getValue().getStopPointList().size()-1).equals(endPoint))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

    }

    /***
     * Create a new route object with general informations
     * @param objectId
     *          the Id of the new route
     * @param objectVersion
     *          the version of the new route
     * @param creationTime
     *          the creation time of the new route
     * @param name
     *          the name of the new route
     * @param routeExt
     *          the route extension of the new route
     * @return
     *          a newly created route with informations given as parameter
     */
    private ChouetteRoute createRouteWithGeneralInfo(String objectId, BigInteger objectVersion, XMLGregorianCalendar creationTime, String name, RouteExtension routeExt){
        ChouetteRoute chouetteRoute = new ChouetteRoute();
        chouetteRoute.setObjectId(objectId);
        chouetteRoute.setObjectVersion(objectVersion);
        chouetteRoute.setCreationTime(creationTime);
        chouetteRoute.setName(name);
        chouetteRoute.setRouteExtension(routeExt);
        return chouetteRoute;
    }

    /***
     * Get all broken routes existing in the network given as parameter.
     * (a break is found in a route when the ending point of a ptLink does not match the starting point of the newt ptLink)
     * @param chouetteNetwork
     *          root object that contains all data of the line
     * @return
     *          a list of routes that contains breaks
     */
    private List<ChouetteRoute> getBrokenRoutes(ChouettePTNetworkType chouetteNetwork){
        buildPtLinkMap(chouetteNetwork.getChouetteLineDescription());
        buildJourneyPatternMap(chouetteNetwork.getChouetteLineDescription());

        return chouetteNetwork.getChouetteLineDescription().getChouetteRoute()
                .stream()
                .filter(this::isBrokenRouteExisting)
                .collect(Collectors.toList());
    }

    /***
     * Detects if a route contains breaks.
     * Also feeds ptLinkTypeMap with all startingPtLinks that will be used later in the process
     * @param chouetteRoute
     *          Route that must be checked
     * @return
     *      true :the route contains breaks
     *      false : the route does not contain breaks
     */
    private boolean isBrokenRouteExisting(ChouetteRoute chouetteRoute){
        List<String> ptLinkList = chouetteRoute.getPtLinkId();
        //used later in the process. Contains starting ptLinks of each segment
        //e.g: if there are 2 breaks in the current route, it will contain 3 segments, so 3 startingPtLinks : beginning, start of sub segment2 and start of segment3
        List<String> startingPtLinks = new ArrayList<>();
        //first starting ptLink is always the first ptLink of the route
        startingPtLinks.add(ptLinkList.get(0));

        String lastPoint = null;

        for (String ptLinkId : ptLinkList){
            if (lastPoint != null && !lastPoint.equals(ptLinkTypeMap.get(ptLinkId).getStartOfLink())) {
                log.warn("Broken route detected on route :" + chouetteRoute.getObjectId() + ", before ptLink:" + ptLinkId);
                startingPtLinks.add(ptLinkId);
            }

            lastPoint = ptLinkTypeMap.get(ptLinkId).getEndOfLink();

        }
        startingPtLinksMap.put(chouetteRoute.getObjectId(),startingPtLinks);
        //more than one start means there is at least 1 break in the route
        return startingPtLinks.size() > 1;
    }

    /***
     * Reads all journey Pattern of the network and build a map
     * @param description
     *          description of the network that contains all the journey patterns
     */
    private void buildJourneyPatternMap(ChouettePTNetworkType.ChouetteLineDescription description){
        journeyPatternTypeMap.clear();
        List<JourneyPatternType> journeyPatternList = description.getJourneyPattern();
        journeyPatternList.forEach(journeyPattern->journeyPatternTypeMap.put(journeyPattern.getObjectId(),journeyPattern));
    }

    /***
     * Reads all ptLinks of the network and build a map
     * @param description
     *          description of the network that contains all the ptLinks
     */
    private void buildPtLinkMap(ChouettePTNetworkType.ChouetteLineDescription description){
        ptLinkTypeMap.clear();
        List<PTLinkType> ptLinkList = description.getPtLink();
        ptLinkList.forEach(ptLink->ptLinkTypeMap.put(ptLink.getObjectId(),ptLink));
    }



    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NeptuneBrokenRouteFixerCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NeptuneBrokenRouteFixerCommand.class.getName(), new DefaultCommandFactory());
    }

}