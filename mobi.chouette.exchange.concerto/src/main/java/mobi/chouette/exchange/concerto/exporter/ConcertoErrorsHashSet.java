package mobi.chouette.exchange.concerto.exporter;

import java.util.HashSet;

public class ConcertoErrorsHashSet<E> extends HashSet<E> {

	private static final long serialVersionUID = 1L;
	private Integer count = 0;
	
	@Override
	public boolean add(E e) {
		boolean result = super.add(e);
		if (result) {
			count++;
		}
		return result;
	}
}
