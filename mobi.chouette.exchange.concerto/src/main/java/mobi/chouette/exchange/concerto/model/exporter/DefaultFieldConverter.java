package mobi.chouette.exchange.concerto.model.exporter;

//@todo sch public
   public abstract class DefaultFieldConverter<T> extends FieldConverter<String, T> {
		@SuppressWarnings("rawtypes")
		@Override
		public synchronized T from(Context context, Enum field, String input, T value,
				boolean required) {
			T result = value;
			if (input != null && !input.isEmpty()) {
				try {
					result = convertFrom(input);
				} catch (Exception e) {
					context.put(Context.FIELD, field.name());
					context.put(Context.ERROR,
							ConcertoException.ERROR.INVALID_FORMAT);
					context.put(Context.CODE, "TODO");
					context.put(Context.VALUE, input);
					//@todo sch throw new ConcertoException(context, e);
				}

			} else if (required && value == null) {
				context.put(Context.FIELD, field.name());
				context.put(Context.ERROR, ConcertoException.ERROR.MISSING_FIELD);
				context.put(Context.CODE, "TODO");
				//@todo sch throw new ConcertoException(context);
			}
			return result;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public synchronized String to(Context context, Enum field, T input, boolean required) {
			String result = "";
			if (input != null) {
				try {
					result = convertTo(input);
				} catch (Exception e) {
					context.put(Context.FIELD, field.name());
					context.put(Context.ERROR,
							ConcertoException.ERROR.INVALID_FORMAT);
					context.put(Context.CODE, "TODO");
					context.put(Context.VALUE, input);
					//@todo sch throw new ConcertoException(context, e);
				}
			} else if (required) {
				context.put(Context.FIELD, field.name());
				context.put(Context.ERROR, ConcertoException.ERROR.MISSING_FIELD);
				context.put(Context.CODE, "TODO");
				//@todo sch throw new ConcertoException(context);
			}
			return result;
		}

		protected abstract T convertFrom(String input) throws Exception;

		protected abstract String convertTo(T input) throws Exception;
	}
