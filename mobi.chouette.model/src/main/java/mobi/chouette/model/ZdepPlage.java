package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * zdep_plages
 */

@Entity
@Table(name = "zdep_plages")
@NoArgsConstructor
public class ZdepPlage extends NeptuneObject {

	private static final long serialVersionUID = -4913573673645997434L;

	@Getter
	@Setter
	@GenericGenerator(name = "zdep_plages_id_seq", strategy = "mobi.chouette.persistence.hibernate.ChouetteIdentifierGenerator",
		parameters = {
			@Parameter(name = "sequence_name", value = "zdep_plages_id_seq"),
			@Parameter(name = "increment_size", value = "1") })
	@GeneratedValue(generator = "zdep_plages_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	/**
	 * zdep
	 *
	 * @param zdep
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "zdep")
	private String zdep;

	/**
	 * consumed
	 */
	@Getter
	@Setter
	@Column(name = "consumed")
	private Boolean consumed = false;
}
