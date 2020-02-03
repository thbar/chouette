package mobi.chouette.exchange.concerto.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.util.UUID;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ConcertoOperator extends ConcertoObject implements Serializable
{

   private static final long serialVersionUID = 1L;

   @Setter
   @Getter
   private String type;

   @Getter
   @Setter
   private UUID uuid;

   @Getter
   @Setter
   private LocalDate date;

   @Getter
   @Setter
   private String name;

   @Getter
   @Setter
   private ConcertoObjectId objectId;
}
