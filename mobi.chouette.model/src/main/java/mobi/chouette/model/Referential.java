package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "referentials")
@NoArgsConstructor
@ToString(callSuper = true, exclude = { "accessLinks", "accessPoints",
        "connectionEndLinks", "connectionStartLinks", "containedStopAreas",
        "containedScheduledStopPoints", "routingConstraintAreas", "routingConstraintLines" })

public class Referential extends NeptuneLocalizedObject {
    private static final long serialVersionUID = 4548682479038099240L;

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

    /**
     * set name <br/>
     * truncated to 255 characters if too long
     *
     * @param value
     *            New value
     */
    public void setName(String value) {
        name = StringUtils.abbreviate(value, 255);
    }


    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }
}
