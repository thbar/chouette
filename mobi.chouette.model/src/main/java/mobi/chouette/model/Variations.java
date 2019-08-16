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

@Entity
@Table(name = "variations")
@NoArgsConstructor
public class Variations extends NeptuneObject {

    @Getter
    @Setter
    @GenericGenerator(name = "variations_id_seq", strategy = "mobi.chouette.persistence.hibernate.ChouetteIdentifierGenerator",
            parameters = {
                    @Parameter(name = "sequence_name", value = "variations_id_seq"),
                    @Parameter(name = "increment_size", value = "10")})
    @GeneratedValue(generator = "variations_id_seq")
    @Id
    @Column(name = "id", nullable = false)
    protected Long id;

    /**
     * type
     *
     * @return The actual value
     */
    @Getter
    @Setter
    @Column(name = "typev")
    private String type;

    /**
     * description
     *
     * @return The actual value
     */
    @Getter
    @Setter
    @Column(name = "descriptionv")
    private String description;

    /**
     * job
     *
     * @return The actual value
     */
    @Getter
    @Setter
    @Column(name = "jobv")
    private Long job;
}



