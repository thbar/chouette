package mobi.chouette.exchange.concerto.exporter;


import lombok.Getter;
import lombok.Setter;
import mobi.chouette.model.Company;

import java.util.HashSet;
import java.util.Set;

public class ExportableData extends mobi.chouette.exchange.exporter.ExportableData{

	/**
	 * Companies that are referred to as agencies by gtfs routes.
	 */
	@Getter
	@Setter
	private Set<Company> agencyCompanies = new HashSet<>();

	@Override
	public void clear() {
		super.clear();
		agencyCompanies.clear();
	}
}
