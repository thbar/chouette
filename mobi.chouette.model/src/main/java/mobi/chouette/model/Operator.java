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
 * Operator
 */

@Entity
@Table(name = "operators")
@NoArgsConstructor
public class Operator extends NeptuneObject {

	private static final long serialVersionUID = -808627059588L;


	@Getter
	@Setter
	@GenericGenerator(name = "admin.operators_id_seq", strategy = "mobi.chouette.persistence.hibernate.ChouetteIdentifierGenerator", parameters = {
			@Parameter(name = "sequence_name", value = "admin.operators_id_seq"),
			@Parameter(name = "increment_size", value = "1")})
	@GeneratedValue(generator = "admin.operators_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	/**
	 * schemaName
	 *
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "schema_name")
	private String schemaName;

	/**
	 * name
	 * 
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "name")
	private String name;

	/**
	 * hastusValue
	 *
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "hastus_value")
	private String hastusValue;

	/**
	 * hastusValue
	 *
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "stif_value")
	private String stifValue;
}
