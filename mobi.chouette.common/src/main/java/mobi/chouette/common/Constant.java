package mobi.chouette.common;

public interface Constant {

	public static final boolean ERROR = false;
	public static final boolean SUCCESS = true;

	public static final String INITIAL_CONTEXT = "initial_context";
	public static final String BASE_URI = "base_uri";
	public static final String JOB_ID = "job_id";
	public static final String JOB_DATA = "job_data";
	public static final String ROOT_PATH = "referentials";
	public static final String CONFIGURATION = "configuration";
	public static final String MAPPING_LINE_UUID = "mapping_line_uuid";
	public static final String VALIDATION = "validation";
	public static final String SOURCE = "source";
	public static final String SOURCE_FILE = "source_file";
	public static final String SOURCE_DATABASE = "source_database";

	public static final String OPTIMIZED = "optimized";
	public static final String COPY_IN_PROGRESS = "copy_in_progress";
	public static final String FILE_URL = "file_url";
	public static final String FILE_NAME = "file_name";
	public static final String SCHEMA = "schema";
	public static final String IMPORTER = "importer";
	public static final String EXPORTER = "exporter";
	public static final String VALIDATOR = "validator";	
	public static final String INPUT = "input";
	public static final String OUTPUT = "output";
	public static final String PARAMETERS_FILE = "parameters.json";
	public static final String ACTION_PARAMETERS_FILE = "action_parameters.json";
	public static final String VALIDATION_PARAMETERS_FILE = "validation_parameters.json";
	public static final String REPORT = "report";
	public static final String SAVE_MAIN_VALIDATION_REPORT = "save_main_validation_report";
	public static final String VALIDATION_REPORT = "validation_report";
	public static final String REPORT_FILE = "action_report.json";
	public static final String VALIDATION_FILE = "validation_report.json";
	public static final String CANCEL_ASKED = "cancel_asked";
	public static final String COMMAND_CANCELLED = "command_cancelled";
	public static final String CLEAR_TABLE_CATEGORIES_FOR_LINES = "clear_table_categoriesfor_lines";
	public static final String CLEAR_FOR_IMPORT = "clear_for_import";

	public static final String COLUMN_NUMBER = "column_number";
	public static final String LINE_NUMBER = "line_number";
	// public static final String OBJECT_LOCALISATION = "object_localisation";
	public static final String VALIDATION_CONTEXT = "validation_context";

	public static final String REFERENTIAL = "referential";
	public static final String CACHE = "cache";
	public static final String PARSER = "parser";
	public static final String AREA_BLOC = "area_bloc";
	public static final String CONNECTION_LINK_BLOC = "connection_link_bloc";
	public static final String ALL_SCHEMAS = "all_schemas";

	
	public static final String VALIDATION_DATA = "validation_data";
	public static final String EXPORTABLE_DATA = "exportable_data";
	public static final String SCHEDULED_STOP_POINTS = "scheduled_stop_points";
	public static final String SHARED_DATA_KEYS = "shared_data_keys";
	public static final String SHARED_DATA = "shared_data";
	public static final String METADATA = "metadata";
	public static final String LINE = "line";
	public static final String LINE_ID = "line_id";
	public static final String EXPORTABLE_OPERATORS = "exportable_operators";
	public static final String FEED_INFO = "feed_info";

	public static final char SEP = '|';
	public static final String NULL = "\\N";
	
	public static final String BUFFER = "buffer";

	public static final String CREATION_DATE = "CreationDate";

	public static final String AREA_CENTROID_MAP = "areaCentroidMap";

	public static final String FILE_TO_REFERENTIAL_STOP_ID_MAP = "fileToReferentialStopIdMap";
	public static final String QUAY_TO_STOPPLACE_MAP = "quayToStopPlaceMap";

	public static final String IMPORTED_ID = "imported-id";

	public static final String MOBIITI_PREFIX = "MOBIITI";
	public static final String COLON_REPLACEMENT_CODE="##3A##";
	public static final String SANITIZED_REPLACEMENT_CODE="__3A__";

	public static final String INCOMING_LINE_LIST="incomingLineList";

}
