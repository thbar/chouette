package mobi.chouette.exchange.concerto.model.exporter;

import mobi.chouette.common.JSONUtil;
import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.UUID;

public interface ConcertoConverter {

	DateTimeFormatter BASIC_ISO_DATE =  DateTimeFormat.forPattern("yyyy-MM-dd");

	DefaultFieldConverter<String> STRING_CONVERTER = new DefaultFieldConverter<String>() {

		@Override
		protected String convertFrom(String input) {
			return input.trim();
		}

		@Override
		protected String convertTo(String input) {
			return (input != null) ? input : "";
		}

	};

	DefaultFieldConverter<ConcertoObjectId> OBJECT_ID_CONVERTER = new DefaultFieldConverter<ConcertoObjectId>() {

		@Override
		protected ConcertoObjectId convertFrom(String input) throws Exception {
			return JSONUtil.fromJSON(input, ConcertoObjectId.class);
		}

		@Override
		protected String convertTo(ConcertoObjectId input) throws Exception {
			String json = JSONUtil.toJSON(input);
			String val = (new JSONObject(json)).getString("root");
			val = val.replace("\\\"", "\"");
			val = val.replaceAll("\"\"", "\"");
			return val;
		}

	};

	DefaultFieldConverter<UUID[]> UUID_ARRAY_CONVERTER = new DefaultFieldConverter<UUID[]>() {

		@Override
		protected UUID[] convertFrom(String input) throws Exception {
			return JSONUtil.fromJSON(input, UUID[].class);
		}

		@Override
		protected String convertTo(UUID[] input) {
			JSONArray jsonArray = new JSONArray(Arrays.asList(input));
			return jsonArray.toString();
		}

	};

	DefaultFieldConverter<UUID> UUID_CONVERTER = new DefaultFieldConverter<UUID>() {

		@Override
		protected UUID convertFrom(String input) {
			return UUID.fromString(input);
		}

		@Override
		protected String convertTo(UUID input) {
			return (input != null) ? input.toString() : "";
		}
	};

	DefaultFieldConverter<Boolean> BOOLEAN_CONVERTER = new DefaultFieldConverter<Boolean>() {

		@Override
		protected Boolean convertFrom(String input) {
			boolean value = input.equals("0");
			if (value) {
				return false;
			} else {
				value = input.equals("1");
				if (value) {
					return true;
				} else {

					throw new IllegalArgumentException();
				}
			}
		}

		@Override
		protected String convertTo(Boolean input) {
			return (input != null) ? (input) ? "true" : "false" : "false";
		}

	};

	DefaultFieldConverter<LocalDate> DATE_CONVERTER = new DefaultFieldConverter<LocalDate>() {

		@Override
		protected LocalDate convertFrom(String input) {
			return LocalDate.parse(input,BASIC_ISO_DATE);
		}

		@Override
		protected String convertTo(LocalDate input) {
			return (input != null) ? BASIC_ISO_DATE.print(input) : "";
		}

	};


}


