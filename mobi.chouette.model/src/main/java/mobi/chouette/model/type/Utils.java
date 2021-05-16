package mobi.chouette.model.type;

import mobi.chouette.model.StopArea;

import java.util.EnumSet;

/**
 * tools on enums
 */
public class Utils
{

   /**
    * find enum value ignoring case
    * 
    * @param text enum value name
    * @param cls enum class name
    * @return enum found (null if not found)
    */
   public static <T extends Enum<T>> T valueOfIgnoreCase(String text,
         Class<T> cls)
   {
      T result = null;
      for (T item : EnumSet.allOf(cls))
      {
         if (item.name().equalsIgnoreCase(text))
         {
            result = item;
            break;
         }
      }
      return result;
   }

   public static void copyStopArea(StopArea src, StopArea dest){
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
}


