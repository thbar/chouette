package mobi.chouette.exchange.concerto.model.exporter;

import mobi.chouette.exchange.concerto.model.ConcertoLine;
import mobi.chouette.exchange.concerto.model.ConcertoOperator;
import mobi.chouette.exchange.concerto.model.ConcertoStopArea;

public interface ConcertoExporterInterface {
	Exporter<ConcertoOperator> getOperatorExporter() throws Exception;

	Exporter<ConcertoStopArea> getStopAreaExporter() throws Exception;

	Exporter<ConcertoLine> getLineExporter() throws Exception;

}
