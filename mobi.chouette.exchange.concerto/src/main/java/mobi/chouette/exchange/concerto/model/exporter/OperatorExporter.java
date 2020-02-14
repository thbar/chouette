package mobi.chouette.exchange.concerto.model.exporter;

import mobi.chouette.exchange.concerto.model.ConcertoOperator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OperatorExporter extends ExporterImpl<ConcertoOperator> implements
		ConcertoConverter {

	public enum FIELDS {
		type, uuid, date, name, object_id
	}

	public static final String NUMBER = "1";
	public static final String FILENAME = "operator";

	public OperatorExporter(String path) throws IOException {
		super(path);
	}

	@Override
	public void writeHeader() {
		// swallow
	}

	@Override
	public void export(ConcertoOperator bean) throws IOException {
		write(CONVERTER.to(_context, bean));
	}

	public static Converter<String, ConcertoOperator> CONVERTER = new Converter<String, ConcertoOperator>() {

		@Override
		public ConcertoOperator from(Context context, String input) {
			ConcertoOperator bean = new ConcertoOperator();
			return bean;
		}

		@Override
		public String to(Context context, ConcertoOperator input) {
			String result = null;
			List<String> values = new ArrayList<String>();
			values.add(STRING_CONVERTER.to(context, FIELDS.type,
					input.getType(), false));

			values.add(UUID_CONVERTER.to(context, FIELDS.uuid,
					input.getUuid(), false));

			values.add(DATE_CONVERTER.to(context, FIELDS.date,
					input.getDate(), false));

			values.add(STRING_CONVERTER.to(context, FIELDS.name,
					input.getName(), false));

			values.add(OBJECT_ID_CONVERTER.to(context, FIELDS.object_id,
					input.getObjectId(), false));

			result = Tokenizer.untokenize(values);
			return result;
		}

	};

	public static class DefaultExporterFactory extends ExporterFactory {

		@Override
		protected Exporter<ConcertoOperator> create(String path) throws IOException {
			return new OperatorExporter(path);
		}
	}

	static {
		ExporterFactory factory = new DefaultExporterFactory();
		ExporterFactory.factories.put(OperatorExporter.class.getName(), factory);
	}

}