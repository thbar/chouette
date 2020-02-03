package mobi.chouette.exchange.concerto.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mobi.chouette.exchange.concerto.exporter.ConcertoErrorsHashSet;
import mobi.chouette.exchange.concerto.exporter.ConcertoExceptionsHashSet;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoException;

import java.util.Set;

@ToString
@NoArgsConstructor
public abstract class ConcertoObject
{
   @Getter
   protected Set<ConcertoException> errors = new ConcertoExceptionsHashSet<>();
   
   @Getter
   protected Set<ConcertoException.ERROR> okTests = new ConcertoErrorsHashSet<>();
}
