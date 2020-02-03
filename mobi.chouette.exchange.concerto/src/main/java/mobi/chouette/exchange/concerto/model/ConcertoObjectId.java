package mobi.chouette.exchange.concerto.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "root")
public class ConcertoObjectId implements Serializable
{

    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private String hastus;

    @Getter
    @Setter
    private String stif;
}
