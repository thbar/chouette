package mobi.chouette.exchange.gtfs.exporter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.gtfs.parameters.IdFormat;
import mobi.chouette.exchange.parameters.AbstractExportParameter;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "gtfs-export")
@NoArgsConstructor
@ToString(callSuper=true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={"objectIdPrefix","timeZone","keepOriginalId","useTpegHvt","exportedFileName","stopIdPrefix","lineIdPrefix","idFormat","idSuffix","commercialPointIdPrefix"})

public class GtfsExportParameters  extends AbstractExportParameter {
		
	@Getter @Setter
	@XmlElement(name = "time_zone",required = true)
	private String timeZone;
	
	@Getter @Setter
	@XmlElement(name = "object_id_prefix",required = true)
	private String objectIdPrefix;
	
	@Getter @Setter
	@XmlElement(name = "keep_original_id",required = false)
	private boolean keepOriginalId = false;
	
	@Getter @Setter
	@XmlElement(name = "use_tpeg_hvt",required = false)
	private boolean useTpegHvt = false;

	@Getter @Setter
	@XmlElement(name = "exported_filename",required = false)
	private String exportedFileName;

	@Getter @Setter
	@XmlElement(name = "stop_id_prefix",required = false)
	private String stopIdPrefix;

	@Getter @Setter
	@XmlElement(name = "line_id_prefix",required = false)
	private String lineIdPrefix;

	@Getter @Setter
	@XmlElement(name = "id_format",required = false)
	private IdFormat idFormat;

	@Getter @Setter
	@XmlElement(name = "id_suffix",required = false)
	private String idSuffix;

	@Getter @Setter
	@XmlElement(name = "commercial_point_id_prefix",required = false)
	private String commercialPointIdPrefix;


	public boolean isValid(Logger log, String[] allowedTypes)
	{
		if (!super.isValid(log,allowedTypes)) return false;

		if (timeZone == null || timeZone.isEmpty()) {
			log.error("missing time_zone");
			return false;
		}

		return true;
		
	}
}
