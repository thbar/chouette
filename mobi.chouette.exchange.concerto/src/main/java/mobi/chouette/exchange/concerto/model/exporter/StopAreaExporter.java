package mobi.chouette.exchange.concerto.model.exporter;

import mobi.chouette.exchange.concerto.model.ConcertoStopArea;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StopAreaExporter extends ExporterImpl<ConcertoStopArea> implements
		ConcertoConverter {

	public enum FIELDS {
		type, uuid, parent_id, referent_id, date, name, object_id, line_ids, attributes, references, collected_always, collect_children, collect_general_messages
	}

	public static final String NUMBER = "3";
	public static final String FILENAME = "stop_area";

	public StopAreaExporter(String path) throws IOException {
		super(path);
	}

	@Override
	public void writeHeader() {
		// swallow
	}

	@Override
	public void export(ConcertoStopArea bean) throws IOException {
		write(CONVERTER.to(_context, bean));
	}

	public static Converter<String, ConcertoStopArea> CONVERTER = new Converter<String, ConcertoStopArea>() {

		@Override
		public ConcertoStopArea from(Context context, String input) {
			ConcertoStopArea bean = new ConcertoStopArea();
			return bean;
		}

		@Override
		public String to(Context context, ConcertoStopArea input) {
			String result = null;
			List<String> values = new ArrayList<String>();
			values.add(STRING_CONVERTER.to(context, FIELDS.type,
					input.getType(), false));
			values.add(UUID_CONVERTER.to(context, FIELDS.uuid,
					input.getUuid(), false));
			values.add(UUID_CONVERTER.to(context, FIELDS.parent_id,
					input.getParent_uuid(), false));
			values.add(UUID_CONVERTER.to(context, FIELDS.referent_id,
					input.getReferent_uuid(), false));
			values.add(DATE_CONVERTER.to(context, FIELDS.date,
					input.getDate(), true));
			values.add(STRING_CONVERTER.to(context, FIELDS.name,
					input.getName(), false));
			values.add(OBJECT_ID_CONVERTER.to(context, FIELDS.object_id,
					input.getObjectId(), true));
			values.add(UUID_ARRAY_CONVERTER.to(context, FIELDS.line_ids,
					input.getLines(), true));
			values.add(STRING_CONVERTER.to(context, FIELDS.attributes,
					input.getAttributes(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.references,
					input.getReferences(), false));
			values.add(BOOLEAN_CONVERTER.to(context, FIELDS.collected_always,
					input.getCollectedAlways(), true));
			values.add(BOOLEAN_CONVERTER.to(context, FIELDS.collect_children,
					input.getCollectChildren(), true));
			values.add(BOOLEAN_CONVERTER.to(context, FIELDS.collect_general_messages,
					input.getCollectGeneralMessages(), true));


			result = Tokenizer.untokenize(values);
			return result;
		}

	};

	public static class DefaultExporterFactory extends ExporterFactory {

		@Override
		protected Exporter<ConcertoStopArea> create(String path) throws IOException {
			return new StopAreaExporter(path);
		}
	}

	static {
		ExporterFactory factory = new DefaultExporterFactory();
		ExporterFactory.factories.put(StopAreaExporter.class.getName(), factory);
	}

}