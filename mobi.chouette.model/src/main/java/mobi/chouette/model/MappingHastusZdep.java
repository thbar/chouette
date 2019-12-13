package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * mapping_hastus_zdep
 */

@Entity
@Table(name = "mapping_hastus_zdep")
@NoArgsConstructor
public class MappingHastusZdep extends NeptuneObject {

	private static final long serialVersionUID = -491573613645997434L;

	@Getter
	@Setter
	@SequenceGenerator(name = "mapping_hastus_zdep_id_seq", sequenceName = "mapping_hastus_zdep_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mapping_hastus_zdep_id_seq")

	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	/**
	 * referential
	 *
	 * @param referential
	 * New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "referential")
	private String referential;

	/**
	 * zdep
	 *
	 * @param zdep
	 * New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "zdep")
	private String zdep;

	/**
	 * zder
	 *
	 * @param zder
	 * New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "zder")
	private String zder;

	/**
	 * zdel
	 *
	 * @param zdel
	 * New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "zdlr")
	private String zdlr;

	/**
	 * hastusChouette
	 *
	 * @param hastusChouette
	 * New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "hastus_chouette")
	private String hastusChouette;

	/**
	 * hastusChouette
	 *
	 * @param hastusChouette
	 * New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "hastus_original")
	private String hastusOriginal;

}


