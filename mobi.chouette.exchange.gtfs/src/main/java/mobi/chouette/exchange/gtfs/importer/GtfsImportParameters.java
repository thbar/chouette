package mobi.chouette.exchange.gtfs.importer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractImportParameter;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.*;
import java.util.Arrays;

@XmlRootElement(name = "gtfs-import")
@NoArgsConstructor
@ToString(callSuper=true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={"objectIdPrefix",
		"splitIdOnDot",
		"maxDistanceForConnectionLink",
		"maxDistanceForCommercial",
		"ignoreEndChars",
		"ignoreLastWord",
		"referencesType",
		"parseInterchanges",
		"parseConnectionLinks",
		"routeMerge",
		"splitCharacter",
		"commercialPointIdPrefixToRemove",
		"quayIdPrefixToRemove"

})
public class GtfsImportParameters extends AbstractImportParameter {

	@Getter@Setter
	@XmlElement(name = "object_id_prefix", required=true)
	private String objectIdPrefix;

	@Getter@Setter
	@XmlElement(name = "split_id_on_dot", defaultValue="true")
	private boolean splitIdOnDot = true;

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
	@XmlElement(name = "parse_interchanges")
	private boolean parseInterchanges = false;

	@Getter@Setter
	@XmlElement(name = "parse_connection_links")
	private boolean parseConnectionLinks = true;

	@Getter@Setter
	@XmlElement(name = "route_merge")
	public Boolean routeMerge = false;

	@Getter@Setter
	@XmlElement(name = "split_character")
	public String splitCharacter = "";

	@Getter@Setter
	@XmlElement(name = "commercial_point_prefix_to_remove")
	public String commercialPointIdPrefixToRemove = "";

	@Getter@Setter
	@XmlElement(name = "quay_id_prefix_to_remove")
	public String quayIdPrefixToRemove = "";


	public boolean isValid(Logger log, String[] allowedTypes)
	{
		if (!super.isValid(log)) return false;
		
		if (objectIdPrefix == null || objectIdPrefix.isEmpty()) {
			log.error("missing object_id_prefix");
			return false;
		}

		if (referencesType != null && !referencesType.isEmpty()) {
			if (!Arrays.asList(allowedTypes).contains(referencesType.toLowerCase())) {
				log.error("invalid type " + referencesType);
				return false;
			}
		}
		return true;

	}
}
