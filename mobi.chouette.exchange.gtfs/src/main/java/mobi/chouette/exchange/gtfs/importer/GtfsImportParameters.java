package mobi.chouette.exchange.gtfs.importer;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractImportParameter;

import org.apache.log4j.Logger;

@XmlRootElement(name = "gtfs-import")
@NoArgsConstructor
@ToString(callSuper=true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={"objectIdPrefix",
		"maxDistanceForConnectionLink",
		"maxDistanceForCommercial",
		"ignoreEndChars",
		"ignoreLastWord",
		"referencesType",
		"routeTypeIdScheme"})
public class GtfsImportParameters extends AbstractImportParameter {

	@Getter@Setter
	@XmlElement(name = "object_id_prefix", required=true)
	private String objectIdPrefix;

	@Getter@Setter
	@XmlElement(name = "max_distance_for_connection_link", defaultValue="0")
	private int maxDistanceForConnectionLink = 0;

	@Getter@Setter
	@XmlElement(name = "max_distance_for_commercial", defaultValue="0")
	private int maxDistanceForCommercial = 0;

	@Getter@Setter
	@XmlElement(name = "ignore_end_chars", defaultValue="0")
	private int ignoreEndChars = 0;

	@Getter@Setter
	@XmlElement(name = "ignore_last_word", defaultValue="false")
	private boolean ignoreLastWord = false;

	@Getter@Setter
	@XmlElement(name = "references_type")
	private String referencesType;

	@Getter@Setter
	@XmlElement(name = "route_type_id_scheme", defaultValue = "standard")
	private String routeTypeIdScheme;

	public boolean isValid(Logger log, String[] allowedReferenceTypes, String[] allowedRouteTypeIdSchemes)
	{
		if (!super.isValid(log)) return false;
		
		if (objectIdPrefix == null || objectIdPrefix.isEmpty()) {
			log.error("missing object_id_prefix");
			return false;
		}

		if (referencesType != null && !referencesType.isEmpty()) {
			if (!Arrays.asList(allowedReferenceTypes).contains(referencesType.toLowerCase())) {
				log.error("invalid type " + referencesType);
				return false;
			}
		}

		if (routeTypeIdScheme != null && !routeTypeIdScheme.isEmpty()) {
			if (!Arrays.asList(allowedRouteTypeIdSchemes).contains(routeTypeIdScheme.toLowerCase())) {
				log.error("invalid route type id scheme " + routeTypeIdScheme);
				return false;
			}
		}

		return true;

	}
}
