package mobi.chouette.exchange.gtfs.parameters;

public class IdParameters {

    private String idPrefix;
    private IdFormat idFormat;
    private String idSuffix;

    public IdParameters() {
    }

    public IdParameters(String idPrefix, IdFormat idFormat, String idSuffix) {
        this.idPrefix = idPrefix;
        this.idFormat = idFormat;
        this.idSuffix = idSuffix;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public IdFormat getIdFormat() {
        return idFormat;
    }

    public void setIdFormat(IdFormat idFormat) {
        this.idFormat = idFormat;
    }

    public String getIdSuffix() {
        return idSuffix;
    }

    public void setIdSuffix(String idSuffix) {
        this.idSuffix = idSuffix;
    }
}
