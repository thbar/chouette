package mobi.chouette.exchange.netexprofile.model;

public class NetexProfileVersion {
	
	//1.04:NO-NeTEx-networktimetable:1.0
	public static String getSchemaVersion(String fullProfileString) {
		String[] split = fullProfileString.split(":");
	
		if(split.length == 3) {
			// Valid
			return split[0];
		}
		return null;
	}

	// TODO getSchema pour profil IDFM
	public static String getSchemaVersionIDFM(String fullProfileString) {
		String[] split = fullProfileString.split(":");

		if(split.length == 2) {
			// Valid
			return split[0];
		}
		return null;
	}

}
