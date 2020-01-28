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
public class ConcertoLine extends ConcertoObject implements Serializable
{

   private static final long serialVersionUID = 1L;

   @Getter
   @Setter
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

   @Getter
   @Setter
   private String attributes;

   @Getter
   @Setter
   private String references;

   @Getter
   @Setter
   private Boolean collectedAlways = true;

   @Getter
   @Setter
   private Boolean collectChildren = true;

   @Getter
   @Setter
   private Boolean collectGeneralMessages = true;
}
