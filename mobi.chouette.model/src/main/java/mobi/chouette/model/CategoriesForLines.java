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
 * CategoriesForLines
 */

@Entity
@Table(name = "categories_for_lines")
@NoArgsConstructor
public class CategoriesForLines extends NeptuneObject {

	private static final long serialVersionUID = -808629127059588L;

	@Getter
	@Setter
	@SequenceGenerator(name = "categories_for_lines_id_seq", sequenceName = "categories_for_lines_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_for_lines_id_seq")

	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	/**
	 * name
	 * 
	 * @return The actual value
	 */
	@Getter
	@Column(name = "name")
	private String name;

}
