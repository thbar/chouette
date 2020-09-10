/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter.producer;

import mobi.chouette.exchange.gtfs.model.GtfsStop;
import mobi.chouette.exchange.gtfs.model.GtfsStop.WheelchairBoardingType;
import mobi.chouette.exchange.gtfs.model.RouteTypeEnum;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.TimeZone;

/**
 * convert Timetable to Gtfs Calendar and CalendarDate
 * <p>
 * optimise multiple period timetable with calendarDate inclusion or exclusion
 */
public class GtfsStopProducer extends AbstractProducer
{
	GtfsStop stop = new GtfsStop();


	public GtfsStopProducer(GtfsExporterInterface exporter)
	{
		super(exporter);
	}

    public boolean save(StopArea neptuneObject, String prefix, Collection<StopArea> validParents, boolean keepOriginalId, boolean useTPEGRouteTypes){
		String stopId = toGtfsId(neptuneObject.getObjectId(), prefix, keepOriginalId);
		if(StringUtils.isEmpty(neptuneObject.getOriginalStopId()) || stopId.contains(".")){
			stopId = neptuneObject.getOriginalStopId();
		}
        return save(neptuneObject, prefix, validParents, keepOriginalId, useTPEGRouteTypes, stopId);
    }

	public boolean save(StopArea neptuneObject, String prefix, Collection<StopArea> validParents, boolean keepOriginalId, boolean useTPEGRouteTypes, String newStopId)
	{
		Optional<StopArea> parent = Optional.ofNullable(neptuneObject.getParent());
		if(validParents != null && !validParents.isEmpty() && parent.isPresent()){
			if(parent.get().getObjectId() == neptuneObject.getObjectId()) {
				return false;
			}
		}

		ChouetteAreaEnum chouetteAreaType = neptuneObject.getAreaType();
		if (chouetteAreaType.compareTo(ChouetteAreaEnum.BoardingPosition) == 0)
			stop.setLocationType(GtfsStop.LocationType.Stop);
		else if (chouetteAreaType.compareTo(ChouetteAreaEnum.Quay) == 0)
			stop.setLocationType(GtfsStop.LocationType.Stop);
		else if (chouetteAreaType.compareTo(ChouetteAreaEnum.CommercialStopPoint) == 0)
			stop.setLocationType(GtfsStop.LocationType.Station);
		// else if(chouetteAreaType.compareTo(ChouetteAreaEnum.STOPPLACE) == 0)
		// stop.setLocationType(GtfsStop.STATION);
		else
			return false; // StopPlaces and ITL type not available

		stop.setStopId(newStopId);

		
		// If name is empty, try to use parent name
		String name = neptuneObject.getName();
		if (name == null && neptuneObject.getParent() != null) {
				name = neptuneObject.getParent().getName();
		}
	
		if(name == null) {
			//         GtfsReportItem item = new GtfsReportItem(
			//               GtfsReportItem.KEY.MISSING_DATA, STATE.ERROR, "StopArea",
			//               neptuneObject.getObjectId(), "Name");
			//         report.addItem(item);
			return false;
		}
		stop.setStopName(neptuneObject.getName());

		if (neptuneObject.getLatitude() == null)
		{
			//         GtfsReportItem item = new GtfsReportItem(
			//               GtfsReportItem.KEY.MISSING_DATA, STATE.ERROR, "StopArea",
			//               neptuneObject.getName(), "Latitude");
			//         report.addItem(item);
			return false;
		}
		stop.setStopLat(neptuneObject.getLatitude());
		if (neptuneObject.getLongitude() == null)
		{
			//         GtfsReportItem item = new GtfsReportItem(
			//               GtfsReportItem.KEY.MISSING_DATA, STATE.ERROR, "StopArea",
			//               neptuneObject.getName(), "Longitude");
			//         report.addItem(item);
			return false;
		}
		stop.setStopLon(neptuneObject.getLongitude());
		stop.setStopCode(neptuneObject.getRegistrationNumber());
		
		// name and description must be different
//		if (neptuneObject.getName().equals(neptuneObject.getComment()))
//		{
//			stop.setStopDesc(null);
//		}
//		else
//		{
		stop.setStopDesc(neptuneObject.getComment());
//		}
		stop.setStopUrl(getUrl(neptuneObject.getUrl()));
		// manage stop_timezone
		stop.setStopTimezone(null);
		if (!isEmpty(neptuneObject.getTimeZone()))
		{
			TimeZone tz = TimeZone.getTimeZone(neptuneObject.getTimeZone());
			if (tz != null)
			{
				stop.setStopTimezone(tz);
			}
		}

		stop.setParentStation(null);
		if (stop.getLocationType().equals(GtfsStop.LocationType.Stop))
		{
			if (neptuneObject.getParent() != null && validParents.contains(neptuneObject.getParent()))
			{
				stop.setParentStation(toGtfsId(neptuneObject.getParent()
						.getObjectId(),prefix, keepOriginalId));
			}
		}

		if (neptuneObject.getMobilityRestrictedSuitable() != null)
		{
			if (neptuneObject.getMobilityRestrictedSuitable())
			{
				stop.setWheelchairBoarding(WheelchairBoardingType.Allowed);
			}
			else
			{
				stop.setWheelchairBoarding(WheelchairBoardingType.NoAllowed);
			}
		}
		else
		{
			stop.setWheelchairBoarding(WheelchairBoardingType.NoInformation);
		}
		
		stop.setPlatformCode(neptuneObject.getPlatformCode());

		
	      if (neptuneObject.getTransportModeName() != null)
	      {
	         if(useTPEGRouteTypes) {
	        	 stop.setVehicleType(RouteTypeEnum.from(neptuneObject.getTransportModeName(), null));
	         } else {
	    	  
	             switch (neptuneObject.getTransportModeName())
	             {
	             case Tram:
	                stop.setVehicleType(RouteTypeEnum.Tram);
	                break;
	             case Metro:
	                stop.setVehicleType(RouteTypeEnum.Subway);
	                break;
	             case Rail:
	                stop.setVehicleType(RouteTypeEnum.Rail);
	                break;
	             case Water:
	             case Ferry:
	                stop.setVehicleType(RouteTypeEnum.Ferry);
	                break;
	             case Funicular:
	            	 stop.setVehicleType(RouteTypeEnum.Funicular);
	            	 break;
	             case Cableway:
	            	 stop.setVehicleType(RouteTypeEnum.Gondola);
	            	 break;
	             case TrolleyBus:
	             case Coach:
	             case Bus:
	             default:
	                stop.setVehicleType(RouteTypeEnum.Bus);
	             }
	         }
	      
	      }
	      else
	      {
	         stop.setVehicleType(null);
	      }

		
		try
		{
			getExporter().getStopExporter().export(stop);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
