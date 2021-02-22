package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "client", schema = "admin")
@Getter
@Setter
@NoArgsConstructor
public class Provider extends NeptuneObject {

    @Id
    @SequenceGenerator(name = "admin.CLIENT_ID_SEQUENCE", sequenceName = "admin.CLIENT_ID_SEQUENCE", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin.CLIENT_ID_SEQUENCE")
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "schema_name")
    private String schemaName;

    @Column(name = "is_idfm")
    private boolean idfm = true;

    @Column(name = "code_idfm")
    private String codeIdfm;

    @Column(name = "object_type_concerto")
    private String objectTypeConcerto;

    @Column(name = "period_concerto")
    private Integer periodConcerto;
}
