package mobi.chouette.exchange.concerto.model.exporter;

import mobi.chouette.common.Context;

import java.io.IOException;

public interface Exporter<T> {
	void dispose(Context context) throws IOException;

	void writeHeader() throws IOException;

	void export(T bean) throws IOException;

	void write(String text) throws IOException;

}
