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

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "agency")
public class Agency extends NeptuneObject {

	@Id
	private Long id;

	@Column(name = "agency_id")
	private String agencyId;

	@Column(name = "name")
	private String name;

	@Column(name = "url")
	private String url;

	@Column(name = "timezone")
	private String timeZone;

	@Column(name = "lang")
	private String lang;

	@Column(name = "phone")
	private String phone;

	@Column(name = "fare_url")
	private String fareUrl;

	@Column(name = "email")
	private String email;

}
