/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.gtfs.model.GtfsAgency;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.model.Agency;
import mobi.chouette.model.Company;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static mobi.chouette.common.PropertyNames.GTFS_AGENCY_PHONE_DEFAULTS;
import static mobi.chouette.common.PropertyNames.GTFS_AGENCY_URL_DEFAULTS;

/**
 * convert Timetable to Gtfs Calendar and CalendarDate
 * <p>
 * optimise multiple period timetable with calendarDate inclusion or exclusion
 */
@Log4j
public class GtfsAgencyProducer extends AbstractProducer
{
   public GtfsAgencyProducer(GtfsExporterInterface exporter)
   {
      super(exporter);
   }

   private GtfsAgency gtfsAgency = new GtfsAgency();


    public boolean save(Agency agency, String prefix, TimeZone timeZone, boolean keepOriginalId) {
        gtfsAgency.setAgencyId(agency.getAgencyId());

        String name = agency.getName();
        if (name == null || name.trim().isEmpty()) {
            log.error("The agency has no name");
            Context context = new Context();
            context.put(Context.ERROR, GtfsException.ERROR.MISSING_REQUIRED_FIELDS);
            context.put(Context.FIELD, "Agency name");
            throw new GtfsException(context);
        }

        gtfsAgency.setAgencyName(name);

        if(agency.getTimeZone() == null) {
            log.error("The agency has no time zone");
            Context context = new Context();
            context.put(Context.ERROR, GtfsException.ERROR.MISSING_REQUIRED_FIELDS);
            context.put(Context.FIELD, "Agency time zone");
            throw new GtfsException(context);
        }

        gtfsAgency.setAgencyTimezone(TimeZone.getTimeZone(agency.getTimeZone()));

        try {
            gtfsAgency.setAgencyUrl(new URL(sanitizeUrl(getValue(agency.getUrl()))));
        } catch (MalformedURLException e) {
            log.error("The agency has a malformed URL: " + agency.getUrl());
            Context context = new Context();
            context.put(Context.ERROR, GtfsException.ERROR.MISSING_REQUIRED_FIELDS);
            context.put(Context.FIELD, "Agency URL");
            throw new GtfsException(context);
        }

        try {
            gtfsAgency.setAgencyFareUrl(new URL(sanitizeUrl(getValue(agency.getFareUrl()))));
        } catch (MalformedURLException e) {
            log.error("The agency has a malformed fare URL: " + agency.getFareUrl());
        }

        gtfsAgency.setAgencyPhone(agency.getPhone());
        gtfsAgency.setAgencyLang(agency.getLang());
        gtfsAgency.setAgencyEmail(agency.getEmail());

        try {
            getExporter().getAgencyExporter().export(gtfsAgency);
        } catch (Exception e) {
            log.error("fail to produce agency " + e.getClass().getName() + " " + e.getMessage());
            return false;
        }
        return true;
    }

	private String sanitizeUrl(String url) {
		String sanitized = url;
		if (sanitized != null) {

			sanitized = sanitized.trim();
			if (!sanitized.toLowerCase().startsWith("http")) {
				sanitized = "http://" + sanitized;
			}
		}
		return sanitized;
	}

	String createURLFromProviderDefaults(Company neptuneObject) {
		String urlDefaults = System.getProperty(GTFS_AGENCY_URL_DEFAULTS);

		String defaultUrl = getDefaultValueForProvider(neptuneObject, urlDefaults);
		if (defaultUrl != null) return sanitizeUrl(defaultUrl);

		return createURLFromOrganisationalUnit(neptuneObject);
	}

	private String getDefaultValueForProvider(Company neptuneObject, String defaultValues) {
		if (defaultValues != null) {
			Map<String, String> urlsPerCodeSpace = Arrays.stream(defaultValues.split(",")).filter(codeSpaceEqualsUrl -> codeSpaceEqualsUrl != null && codeSpaceEqualsUrl.contains("=")).map(codeSpaceEqualsUrl -> codeSpaceEqualsUrl.split("=")).collect(Collectors.toMap(codeSpaceEqualsUrl -> codeSpaceEqualsUrl[0],
					codeSpaceEqualsUrl -> codeSpaceEqualsUrl[1]));
			return urlsPerCodeSpace.get(neptuneObject.objectIdPrefix());

		}
		return null;
	}

	String createPhoneFromProviderDefaults(Company neptuneObject) {
		String urlDefaults = System.getProperty(GTFS_AGENCY_PHONE_DEFAULTS);
		return getDefaultValueForProvider(neptuneObject, urlDefaults);
	}

   String createURLFromOrganisationalUnit(Company neptuneObject) {
      String url;
      if (neptuneObject.getOrganisationalUnit() != null
			&& neptuneObject.getOrganisationalUnit().startsWith("http"))
	  {
		 // urlData = "OrganisationalUnit";
         url = neptuneObject.getOrganisationalUnit();
      } else {
         String hostName = "unknown";
         if (!StringUtils.isEmpty(neptuneObject.getShortName())) {
            hostName = neptuneObject.getShortName();
         } else if (!StringUtils.isEmpty(neptuneObject.getName())) {
            hostName = neptuneObject.getName();
         }

         url = "http://www." + hostName.replaceAll("[^A-Za-z0-9]", "") + ".com";
      }
      return url;
   }

}
