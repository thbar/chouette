package mobi.chouette.exchange.netexprofile.importer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractImportParameter;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "netexprofile-import")
@NoArgsConstructor
@ToString(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "validCodespaces", "parseSiteFrames", "validateAgainstSchema", "validateAgainstProfile" })
public class NetexprofileImportParameters extends AbstractImportParameter {

	// Comma separated list. Example "NSR,http://www.nsr.no,AVI,http://avinor.no,MOR,http://www.mor.no"
	@Getter
	@Setter
	@XmlElement(name = "valid_codespaces", required = false)
	private String validCodespaces;

	@Getter
	@Setter
	@XmlElement(name = "parse_site_frames", required = false)
	private boolean parseSiteFrames = true;

	@Getter
	@Setter
	@XmlElement(name = "validate_against_schema", required = false)
	private boolean validateAgainstSchema = true;

	@Getter
	@Setter
	@XmlElement(name = "validate_against_profile", required = false)
	private boolean validateAgainstProfile = true;

}
