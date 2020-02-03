package mobi.chouette.exchange.concerto.model.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.concerto.model.ConcertoLine;
import mobi.chouette.exchange.concerto.model.ConcertoObject;
import mobi.chouette.exchange.concerto.model.ConcertoOperator;
import mobi.chouette.exchange.concerto.model.ConcertoStopArea;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoException.ERROR;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Log4j
public class ConcertoExporter implements ConcertoExporterInterface {
	public enum EXPORTER {
		OPERATOR, STOP, LINE;
	}

	@Override
	public Exporter<ConcertoOperator> getOperatorExporter() throws Exception {
		return getExporter(EXPORTER.OPERATOR.name(), OperatorExporter.FILENAME, OperatorExporter.class);
	}

	@Override
	public Exporter<ConcertoStopArea> getStopAreaExporter() throws Exception {
		return getExporter(EXPORTER.STOP.name(), StopAreaExporter.FILENAME, StopAreaExporter.class);
	}

	@Override
	public Exporter<ConcertoLine> getLineExporter() throws Exception {
		return getExporter(EXPORTER.LINE.name(), LineExporter.FILENAME, LineExporter.class);
	}


	private String _path;
	private Map<String, Exporter<ConcertoObject>> _map = new HashMap<String, Exporter<ConcertoObject>>();

	public ConcertoExporter(String path) {
		_path = path;
	}

	@SuppressWarnings("rawtypes")
	public void dispose(mobi.chouette.common.Context context) {
		for (Exporter exporter : _map.values()) {
			try {
				exporter.dispose(context);
			} catch (IOException e) {
				log.error(e);
			}
		}
		_map.clear();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Exporter getExporter(String name, String path, Class clazz) throws Exception{
		Exporter result = _map.get(name);

		if (result == null) {
			try {
				result = ExporterFactory.build(Paths.get(_path, path)
						.toString(), clazz.getName());
				_map.put(name, result);
			} catch (ClassNotFoundException | IOException e) {
				Context context = new Context();
				context.put(Context.PATH, _path);
				context.put(Context.ERROR, ERROR.SYSTEM);
				throw new Exception(e);
				//throw new ConcertoException(context, e);
			}

		}
		return result;
	}
}
