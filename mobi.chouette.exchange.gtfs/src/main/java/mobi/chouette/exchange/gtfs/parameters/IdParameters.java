package mobi.chouette.exchange.gtfs.parameters;

public class IdParameters {

    private String stopIdPrefix;
    private IdFormat idFormat;
    private String idSuffix;
    private String lineIdPrefix;

    public IdParameters() {
    }

    public IdParameters(String stopIdPrefix, IdFormat idFormat, String idSuffix, String lineIdPrefix) {
        this.stopIdPrefix = stopIdPrefix;
        this.idFormat = idFormat;
        this.idSuffix = idSuffix;
        this.lineIdPrefix = lineIdPrefix;
    }

    public String getStopIdPrefix() {
        return stopIdPrefix;
    }

    public void setStopIdPrefix(String stopIdPrefix) {
        this.stopIdPrefix = stopIdPrefix;
    }

    public String getLineIdPrefix() {
        return lineIdPrefix;
    }

    public void setLineIdPrefix(String lineIdPrefix) {
        this.lineIdPrefix = lineIdPrefix;
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
