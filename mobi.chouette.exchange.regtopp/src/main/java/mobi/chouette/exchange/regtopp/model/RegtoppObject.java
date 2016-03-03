package mobi.chouette.exchange.regtopp.model;

import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.regtopp.model.importer.RegtoppErrorsHashSet;
import mobi.chouette.exchange.regtopp.model.importer.RegtoppException;
import mobi.chouette.exchange.regtopp.model.importer.RegtoppExceptionsHashSet;

@ToString
@NoArgsConstructor
public abstract class RegtoppObject
{

   @Getter
   @Setter
   protected Integer id;
   
   @Getter
   protected Set<RegtoppException> errors = new RegtoppExceptionsHashSet<>();
   
   @Getter
   protected Set<RegtoppException.ERROR> okTests = new RegtoppErrorsHashSet<>();
}
