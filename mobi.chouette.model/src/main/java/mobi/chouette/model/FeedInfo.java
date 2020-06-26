package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "feed_info")
public class FeedInfo extends NeptuneObject {

	@Id
	private Long id;

	@Column(name = "publisher_name")
	private String publisherName;

	@Column(name = "publisher_url")
	private String publisherUrl;

	@Column(name = "lang")
	private String lang;

	@Column(name = "start_date")
	private Date startDate;

	@Column(name = "end_date")
	private Date endDate;

	@Column(name = "contact_email")
	private String contactEmail;

	@Column(name = "contact_url")
	private String contactUrl;

}
