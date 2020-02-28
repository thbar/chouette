package mobi.chouette.exchange.concerto.model.exporter;

import mobi.chouette.exchange.concerto.model.ConcertoLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LineExporter extends ExporterImpl<ConcertoLine> implements
		ConcertoConverter {

	public enum FIELDS {
		type, uuid, date, name, object_id, attributes, references, collectedAlways
	}

	public static final String NUMBER = "2";
	public static final String FILENAME = "line";


	public LineExporter(String path) throws IOException {
		super(path);
	}

	@Override
	public void writeHeader() {
		// swallow
	}

	@Override
	public void export(ConcertoLine bean) throws IOException {
		write(CONVERTER.to(_context, bean));
	}

	public static Converter<String, ConcertoLine> CONVERTER = new Converter<String, ConcertoLine>() {

		@Override
		public ConcertoLine from(Context context, String input) {
			ConcertoLine bean = new ConcertoLine();
			return bean;
		}

		@Override
		public String to(Context context, ConcertoLine input) {
			String result = null;
			List<String> values = new ArrayList<String>();
			values.add(STRING_CONVERTER.to(context, FIELDS.type,
					input.getType(), true));
			values.add(UUID_CONVERTER.to(context, FIELDS.uuid,
					input.getUuid(), true));
			values.add(DATE_CONVERTER.to(context, FIELDS.date,
					input.getDate(), true));
			values.add(STRING_CONVERTER.to(context, FIELDS.name,
					input.getName(), true));
			values.add(OBJECT_ID_CONVERTER.to(context, FIELDS.object_id,
					input.getObjectId(), true));
			values.add(STRING_CONVERTER.to(context, FIELDS.attributes,
					input.getAttributes(), true));
			values.add(STRING_CONVERTER.to(context, FIELDS.references,
					input.getReferences(), true));
			values.add(BOOLEAN_CONVERTER.to(context, FIELDS.collectedAlways,
					input.getCollectedAlways(), true));

			result = Tokenizer.untokenize(values);
			return result;
		}

	};

	public static class DefaultExporterFactory extends ExporterFactory {

		@Override
		protected Exporter<ConcertoLine> create(String path) throws IOException {
			return new LineExporter(path);
		}
	}

	static {
		ExporterFactory factory = new DefaultExporterFactory();
		ExporterFactory.factories.put(LineExporter.class.getName(), factory);
	}

}