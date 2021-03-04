package mobi.chouette.exchange.neptune.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.XPPUtil;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;

import mobi.chouette.exchange.neptune.importer.NeptuneImportParameters;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.SimpleObjectReference;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.util.ObjectFactory;
import org.xmlpull.v1.XmlPullParser;
import mobi.chouette.model.util.Referential;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j
public class ChouettePTNetworkParser implements Parser, Constant {
	private static final String CHILD_TAG = "ChouettePTNetwork";
	public static final String STOP_POINT_CONTEXT = "StopPoint";
	private static final String CONTAINED_ID = "containedIn";
	public static final String AREA_CENTROID_CONTEXT = "AreaCentroid";
	public static final String CONTAINED_IN = "containedIn";
	public static final String STOP_AREA_CONTEXT = "StopArea";

	public static final String CENTROID_OF_AREA = "centroidOfArea";
	public static final String CONTAINS2 = "contains";
	protected static final String OBJECT_IDS = "encontered_ids";


	@Override
	public void parse(Context context) throws Exception {


		XmlPullParser xpp = (XmlPullParser) context.get(PARSER);

		XPPUtil.nextStartTag(xpp, CHILD_TAG);

		xpp.require(XmlPullParser.START_TAG, null, CHILD_TAG);
		context.put(COLUMN_NUMBER, xpp.getColumnNumber());
		context.put(LINE_NUMBER, xpp.getLineNumber());

		while (xpp.nextTag() == XmlPullParser.START_TAG) {
			if (xpp.getName().equals("PTNetwork")) {
				Parser parser = ParserFactory.create(PTNetworkParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("GroupOfLine")) {
				Parser parser = ParserFactory.create(GroupOfLineParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("Company")) {
				Parser parser = ParserFactory.create(CompanyParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("ChouetteArea")) {
				Parser parser = ParserFactory.create(ChouetteAreaParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("ConnectionLink")) {
				Parser parser = ParserFactory.create(ConnectionLinkParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("Timetable")) {
				Parser parser = ParserFactory.create(TimetableParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("TimeSlot")) {
				Parser parser = ParserFactory.create(TimeSlotParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("ChouetteLineDescription")) {
				Parser parser = ParserFactory
						.create(ChouetteLineDescriptionParser.class.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("Facility")) {
				// TODO [DSU] Facility
				XPPUtil.skipSubTree(log, xpp);
				
			} else if (xpp.getName().equals("AccessPoint")) {
				Parser parser = ParserFactory.create(AccessPointParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("AccessLink")) {
				Parser parser = ParserFactory.create(AccessLinkParser.class
						.getName());
				parser.parse(context);
			} else {
				XPPUtil.skipSubTree(log, xpp);
			}
		}
		mapFileIdsToReferentialIds(context);
		mapIdsInValidationContext(context);
	}

	private String buildTridentId(String referentialName, StopArea tmpStopArea){
		String type = ChouetteAreaEnum.BoardingPosition.equals(tmpStopArea.getAreaType()) ? "Quay" : "StopPlace";
		return referentialName + ":" + type + ":" + tmpStopArea.getOriginalStopId();
	}

	private void copyStopArea(StopArea src, StopArea dest){
		dest.setOriginalStopId(src.getOriginalStopId());
		dest.setAreaType(src.getAreaType());
		dest.setFareCode(src.getFareCode());
		dest.setContainedScheduledStopPoints(src.getContainedScheduledStopPoints());
		dest.setAccessLinks(src.getAccessLinks());
		dest.setAccessPoints(src.getAccessPoints());
		dest.setComment(src.getComment());
		dest.setCompassBearing(src.getCompassBearing());
		dest.setFilled(src.isFilled());
		dest.setObjectVersion(src.getObjectVersion());
		dest.setCreationTime(src.getCreationTime());
		dest.setCreatorId(src.getCreatorId());
		dest.setName(src.getName());
		dest.setComment(src.getComment());
		dest.setAreaType(src.getAreaType());
		dest.setNearestTopicName(src.getNearestTopicName());
		dest.setRegistrationNumber(src.getRegistrationNumber());
		dest.setMobilityRestrictedSuitable(src.getMobilityRestrictedSuitable());
		dest.setUserNeeds(src.getUserNeeds());
		dest.setStairsAvailable(src.getStairsAvailable());
		dest.setLiftAvailable(src.getLiftAvailable());
		dest.setConnectionEndLinks(src.getConnectionEndLinks());
		dest.setConnectionStartLinks(src.getConnectionStartLinks());
		dest.setImportMode(src.getImportMode());
		dest.setIntUserNeeds(src.getIntUserNeeds());
		dest.setContainedStopAreas(src.getContainedStopAreas());
		dest.setParent(src.getParent());
		dest.setIsExternal(src.getIsExternal());
		dest.setPlatformCode(src.getPlatformCode());
		dest.setRoutingConstraintAreas(src.getRoutingConstraintAreas());
		dest.setStopAreaType(src.getStopAreaType());
		dest.setTimeZone(src.getTimeZone());
		dest.setUrl(src.getUrl());
		dest.setX(src.getX());
		dest.setY(src.getY());
		dest.setLatitude(src.getLatitude());
		dest.setLongitude(src.getLongitude());
		dest.setLongLatType(src.getLongLatType());

	}

	private void mapFileIdsToReferentialIds(Context context){

		Referential referential = (Referential) context.get(REFERENTIAL);
		NeptuneImportParameters parameters = (NeptuneImportParameters) context.get(CONFIGURATION);
		Map<String,String> fileToReferentialStopIdMap =  (Map<String,String>) context.get(FILE_TO_REFERENTIAL_STOP_ID_MAP);
		String referentialName = parameters.getReferentialName();

		List<StopArea> oldStopArea = referential.getSharedStopAreas().values().stream()
																			   .filter(stopArea -> !stopArea.getObjectId().startsWith(MOBIITI_PREFIX) &&
																					   				!stopArea.getObjectId().startsWith(referentialName))
																				.collect(Collectors.toList());
		//replace all stopAreas with olfd IDs by stopAreas with new Ids at root level
		for (StopArea stopArea : oldStopArea){
			String newId = buildTridentId(referentialName,stopArea);
			StopArea newStopArea = ObjectFactory.getStopArea(referential, newId);
			copyStopArea(stopArea,newStopArea);
			fileToReferentialStopIdMap.put(stopArea.getObjectId(),newId);
		}


		List<StopArea> newStopAreas = referential.getSharedStopAreas().values().stream()
																			.filter(stopArea ->stopArea.getObjectId().startsWith(referentialName))
																			.collect(Collectors.toList());

		//replace all parents in new stopAreas
		for (StopArea stopArea : newStopAreas){
			StopArea parentArea = stopArea.getParent();
			if (parentArea == null)
				continue;

			String parentId = parentArea.getObjectId();
			String newParentId = fileToReferentialStopIdMap.get(parentId);
			StopArea newParentArea = referential.getSharedStopAreas().get(newParentId);
			stopArea.setParent(newParentArea);
		}

		newStopAreas.forEach(stopArea->mapIdsInContainedInStopAreas(context,stopArea));


		oldStopArea.forEach(oldId->{
			referential.getSharedStopAreas().remove(oldId.getObjectId());
			referential.getStopAreas().remove(oldId.getObjectId());
		});

		List<ScheduledStopPoint> oldScheduledStopPoint = new ArrayList(referential.getScheduledStopPoints().values());
		for (ScheduledStopPoint scheduleStopPoint: oldScheduledStopPoint){
			String oldStopAreaId = scheduleStopPoint.getContainedInStopAreaRef().getObjectId();
			String newStopAreaId = fileToReferentialStopIdMap.get(oldStopAreaId);
			StopArea newStopArea = referential.getSharedStopAreas().get(newStopAreaId);
			scheduleStopPoint.setContainedInStopAreaRef(new SimpleObjectReference(newStopArea));
		}

	}

	private void mapIdsInContainedInStopAreas(Context context, StopArea stopArea){
		List<StopArea> childrenToDelete = new ArrayList<>();
		Map<String,String> fileToReferentialStopIdMap =  (Map<String,String>) context.get(FILE_TO_REFERENTIAL_STOP_ID_MAP);
		Referential referential = (Referential) context.get(REFERENTIAL);


		List<StopArea> containedInStopAreas = new ArrayList<>(stopArea.getContainedStopAreas());
		if (containedInStopAreas.size() == 0)
			return;

		for (StopArea child : containedInStopAreas){

			if (!fileToReferentialStopIdMap.containsKey(child.getObjectId()))
				continue;

			childrenToDelete.add(child);
			String newChildId = fileToReferentialStopIdMap.get(child.getObjectId());
			StopArea newChild = referential.getSharedStopAreas().get(newChildId);
			if (!stopArea.getContainedStopAreas().contains(newChild))
				stopArea.getContainedStopAreas().add(newChild);
		}

		childrenToDelete.forEach(child->stopArea.getContainedStopAreas().remove(child));

	}

	private void mapIdsInValidationContext(Context context){
		Context validationContext = (Context) context.get(VALIDATION_CONTEXT);
		Context stopPointContext = (Context) validationContext.get(STOP_POINT_CONTEXT);
		Map<String,String> fileToReferentialStopIdMap =  (Map<String,String>) context.get(FILE_TO_REFERENTIAL_STOP_ID_MAP);

		for (Map.Entry entry : stopPointContext.entrySet()){
			Context pointContext = (Context) entry.getValue();
			String containedIn = (String) pointContext.get(CONTAINED_ID);

			if (containedIn != null && fileToReferentialStopIdMap.containsKey(containedIn)){
				pointContext.put(CONTAINED_ID,fileToReferentialStopIdMap.get(containedIn));
			}
		}


		Context areaCentroidContext = (Context) validationContext.get(AREA_CENTROID_CONTEXT);


		for (Object objectContext : areaCentroidContext.values()){
				Context areaContext = (Context) objectContext;
				String containedIn = (String) areaContext.get(CONTAINED_IN);
				if (containedIn == null)
					continue;

				if (fileToReferentialStopIdMap.containsKey(containedIn)){
					areaContext.put(CONTAINED_IN,fileToReferentialStopIdMap.get(containedIn));
				}
		}


		Context stopAreaContext = (Context) validationContext.get(STOP_AREA_CONTEXT);
		List<String> oldObjectsToDelete = new ArrayList<>();

		List<String> rawIdsFromfile = fileToReferentialStopIdMap.keySet().stream()
																		 .collect(Collectors.toList());

		List<Map.Entry<String, Object>> oldObjects = stopAreaContext.entrySet().stream()
				.filter(entry -> rawIdsFromfile.contains(entry.getKey()))
				.collect(Collectors.toList());

		oldObjects.forEach(entry -> {
			oldObjectsToDelete.add(entry.getKey());
			updateNewObject(context,entry);
		});

		oldObjectsToDelete.forEach(stopAreaContext::remove);


	}

	private void updateNewObject(Context globalContext, Map.Entry<String, Object> entry){
		Map<String,String> fileToReferentialStopIdMap =  (Map<String,String>) globalContext.get(FILE_TO_REFERENTIAL_STOP_ID_MAP);


		//Gathering data from old object
		Context oldObjectContext =(Context) entry.getValue();
		List<String> oldContains = (List<String>) oldObjectContext.get(CONTAINS2);
		String oldCentroid = (String) oldObjectContext.get(CENTROID_OF_AREA);


		//And put it in the new object
		String newObjectId = fileToReferentialStopIdMap.get(entry.getKey());
		Context newObjectContext =getObjectContext(globalContext,STOP_AREA_CONTEXT,newObjectId);

		List<String> newContains = (List<String>) newObjectContext.get(CONTAINS2);
		if (newContains == null)
		{
			newContains = new ArrayList<>();
			newObjectContext.put(CONTAINS2, newContains);
		}

		if (oldContains != null && !oldContains.isEmpty())
			newContains.addAll(convertContainsToNewIds(fileToReferentialStopIdMap,oldContains));

		if (oldCentroid != null)
			newObjectContext.put(CENTROID_OF_AREA,fileToReferentialStopIdMap.containsKey(oldCentroid) ? fileToReferentialStopIdMap.get(oldCentroid) : oldCentroid);

	}


	private List<String> convertContainsToNewIds(Map<String,String> fileToReferentialStopIdMap, List<String> inputList){
		List<String> convertedList = new ArrayList<>();
		for (String inputId : inputList){
			convertedList.add(fileToReferentialStopIdMap.containsKey(inputId) ? fileToReferentialStopIdMap.get(inputId) : inputId);
		}
		return convertedList;
	}

	protected static Context getObjectContext(Context context, String localContextName, String objectId) {
		Context validationContext = (Context) context.get(VALIDATION_CONTEXT);
		if (validationContext == null) {
			validationContext = new Context();
			context.put(VALIDATION_CONTEXT, validationContext);
			validationContext.put(OBJECT_IDS, new HashSet<String>());
		}

		Set<String> objectIds = (Set<String>) validationContext.get(OBJECT_IDS);
		objectIds.add(objectId);

		Context localContext = (Context) validationContext.get(localContextName);
		if (localContext == null) {
			localContext = new Context();
			validationContext.put(localContextName, localContext);
		}
		Context objectContext = (Context) localContext.get(objectId);
		if (objectContext == null) {
			objectContext = new Context();
			localContext.put(objectId, objectContext);
		}
		return objectContext;

	}



	static {
		ParserFactory.register(ChouettePTNetworkParser.class.getName(),
				new ParserFactory() {
					private ChouettePTNetworkParser instance = new ChouettePTNetworkParser();

					@Override
					protected Parser create() {
						return instance;
					}
				});
	}
}
