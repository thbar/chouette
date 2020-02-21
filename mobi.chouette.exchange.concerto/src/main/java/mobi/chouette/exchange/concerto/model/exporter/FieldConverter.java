package mobi.chouette.exchange.concerto.model.exporter;

//@todo sch public
	public abstract class FieldConverter<F, T> {

		@SuppressWarnings("rawtypes")
		public T from(Context context, Enum field, F input, boolean required) {
			return from(context, field, input, null, required);
		}

		@SuppressWarnings("rawtypes")
		public abstract T from(Context context, Enum field, F input, T value,
				boolean required);

		@SuppressWarnings("rawtypes")
		public abstract F to(Context context, Enum field, T input,
				boolean required);
	}
