package mobi.chouette.exchange.concerto.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang.math.NumberUtils;

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
    private String hastus;
    public void setHastus(String valeur){
        if(NumberUtils.isNumber(valeur)){
            valeur = "\"" + valeur + "\"";
        }
        this.hastus = valeur;
    }

    @Getter
    private String stif;
    public void setStif(String valeur)
    {
        if(NumberUtils.isNumber(valeur)){
            valeur = "\"" + valeur + "\"";
        }
        this.stif = valeur;
    }

}
