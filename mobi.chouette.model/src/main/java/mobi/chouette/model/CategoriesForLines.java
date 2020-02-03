package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
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
