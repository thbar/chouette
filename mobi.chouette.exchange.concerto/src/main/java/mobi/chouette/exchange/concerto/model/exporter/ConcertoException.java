package mobi.chouette.exchange.concerto.model.exporter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
//@EqualsAndHashCode(callSuper=false, exclude={"id", "value"})
@EqualsAndHashCode(callSuper = false)
public class ConcertoException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public enum ERROR {
		MISSING_FIELD,
		INVALID_FORMAT,
		SYSTEM,
		MISSING_REQUIRED_VALUES,
		MISSING_REQUIRED_VALUES2,
		HTML_TAG_IN_HEADER_FIELD,
		DUPLICATE_DOUBLE_KEY,
		MISSING_ARRIVAL_TIME,
		MISSING_DEPARTURE_TIME,
		DUPLICATE_STOP_SEQUENCE,
		MISSING_TRANSFER_TIME,
		UNREFERENCED_ID,
		BAD_REFERENCED_ID,
		COORDINATES_STOP_0_0
	}

	@Getter
	private String path;
	@Getter
	private Integer id;
	@Getter
	private Integer column = new Integer(-1);
	@Getter
	private String field;
	@Getter
	private ERROR error;

	@Getter
	private String code;

	@Getter
	private String value;

	@Getter
	private String refValue;

	public ConcertoException(String path, Integer id, Integer column, String field, ERROR error, String code, String value,
                             String refValue, Throwable cause) {
		super(cause);
		this.path = path;
		this.id = id;
		this.column = column;
		this.field = field;
		this.error = error;
		this.code = code;
		this.value = value;
		this.refValue = refValue;
	}
}
