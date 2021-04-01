package mobi.chouette.exchange.gtfs.model.importer;

public class FactoryParameters {

    String splitCharacter;
    String linePrefixToRemove;

    public String getSplitCharacter() {
        return splitCharacter;
    }

    public void setSplitCharacter(String splitCharacter) {
        this.splitCharacter = splitCharacter;
    }

    public String getLinePrefixToRemove() {
        return linePrefixToRemove;
    }

    public void setLinePrefixToRemove(String linePrefixToRemove) {
        this.linePrefixToRemove = linePrefixToRemove;
    }
}
