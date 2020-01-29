package mobi.chouette.exchange.metadata;


public interface Formater
{
String format(Metadata.Period period);
String format(Metadata.Box box);
String format(Metadata.Resource resource);
}
