package mobi.chouette.exchange.concerto.model.exporter;

public abstract class Converter<F, T> {

    public abstract T from(Context context, F input);

    public abstract F to(Context context, T input);

}
