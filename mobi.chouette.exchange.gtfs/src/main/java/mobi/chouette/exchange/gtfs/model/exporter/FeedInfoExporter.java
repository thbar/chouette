package mobi.chouette.exchange.gtfs.model.exporter;

import mobi.chouette.exchange.gtfs.model.GtfsFeedInfo;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.model.importer.GtfsConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FeedInfoExporter extends ExporterImpl<GtfsFeedInfo> implements
		GtfsConverter {
	public static enum FIELDS {
		feed_publisher_name, feed_publisher_url, feed_lang, feed_start_date, feed_end_date, feed_version, feed_contact_email, feed_contact_url;
	};

	public static final String FILENAME = "feed_info.txt";

	public FeedInfoExporter(String name) throws IOException {
		super(name);
	}

	@Override
	public void writeHeader() throws IOException {
		write(FIELDS.values());
	}

	@Override
	public void export(GtfsFeedInfo bean) throws IOException {
		write(CONVERTER.to(_context, bean));
	}

	public static Converter<String, GtfsFeedInfo> CONVERTER = new Converter<String, GtfsFeedInfo>() {

		@Override
		public GtfsFeedInfo from(Context context, String input) {
			GtfsFeedInfo bean = new GtfsFeedInfo();
			List<String> values = Tokenizer.tokenize(input);

			int i = 0;
			bean.setFeedPublisherName(STRING_CONVERTER.from(context, FIELDS.feed_publisher_name, values.get(i++), true));
			bean.setFeedPublisherUrl(URL_CONVERTER.from(context, FIELDS.feed_publisher_url, values.get(i++), true));
			bean.setFeedLang(STRING_CONVERTER.from(context, FIELDS.feed_lang, values.get(i++), true));
			bean.setFeedStartDate(DATE_CONVERTER.from(context, FIELDS.feed_start_date, values.get(i++), false));
			bean.setFeedEndDate(DATE_CONVERTER.from(context, FIELDS.feed_end_date, values.get(i++), false));
			bean.setFeedVersion(STRING_CONVERTER.from(context, FIELDS.feed_version, values.get(i++), false));
			bean.setFeedContactEmail(STRING_CONVERTER.from(context, FIELDS.feed_contact_email, values.get(i++), false));
			bean.setFeedContactUrl(URL_CONVERTER.from(context, FIELDS.feed_contact_url, values.get(i++), false));

			return bean;
		}

		@Override
		public String to(Context context, GtfsFeedInfo input) {
			String result = null;
			List<String> values = new ArrayList<String>();
			values.add(STRING_CONVERTER.to(context, FIELDS.feed_publisher_name, input.getFeedPublisherName(), true));
			values.add(URL_CONVERTER.to(context, FIELDS.feed_publisher_url, input.getFeedPublisherUrl(), true));
			values.add(STRING_CONVERTER.to(context, FIELDS.feed_lang, input.getFeedLang(), true));
			values.add(DATE_CONVERTER.to(context, FIELDS.feed_start_date, input.getFeedStartDate(), false));
			values.add(DATE_CONVERTER.to(context, FIELDS.feed_end_date, input.getFeedEndDate(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.feed_version, input.getFeedVersion(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.feed_contact_email, input.getFeedContactEmail(), false));
			values.add(URL_CONVERTER.to(context, FIELDS.feed_contact_url, input.getFeedContactUrl(), false));

			result = Tokenizer.untokenize(values);
			return result;
		}

	};

	public static class DefaultExporterFactory extends ExporterFactory {

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected Exporter create(String path) throws IOException {
			return new FeedInfoExporter(path);
		}
	}

	static {
		ExporterFactory factory = new DefaultExporterFactory();
		ExporterFactory.factories.put(FeedInfoExporter.class.getName(), factory);
	}
}
