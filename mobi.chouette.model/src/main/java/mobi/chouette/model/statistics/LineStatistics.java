package mobi.chouette.model.statistics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import mobi.chouette.model.util.DateAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@XmlRootElement(name = "lineStatistics")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "startDate", "days", "validityCategories", "publicLines", "invalid", "expiring" })
@Getter
public class LineStatistics {

	// Use sql date for reliable serialization of date only (Should have been java.time/joda LocalDate)
	@XmlJavaTypeAdapter(DateAdapter.class)
	private java.sql.Date startDate;
	private int days;
	private List<ValidityCategory> validityCategories = new ArrayList<>();
	private List<PublicLine> publicLines = new ArrayList<>();
	private boolean invalid;
	private boolean expiring;

	public LineStatistics(Date startDate, int days, List<PublicLine> publicLines, boolean invalid, boolean expiring) {
		this.startDate = startDate == null ? null : new java.sql.Date(startDate.getTime());
		this.days = days;
		this.publicLines = publicLines;
		this.invalid = invalid;
		this.expiring = expiring;
	}

    public void setExpiring(boolean expiring) {
        this.expiring = expiring;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }
}