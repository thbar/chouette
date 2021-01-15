package mobi.chouette.exchange.gtfs.exporter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.gtfs.importer.IdFormat;
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
@XmlType(propOrder={"objectIdPrefix","timeZone","keepOriginalId","useTpegHvt","idPrefix","idFormat"})

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
	@XmlElement(name = "id_prefix",required = false)
	private String idPrefix;

	@Getter @Setter
	@XmlElement(name = "id_format",required = false)
	private IdFormat idFormat;



	
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
