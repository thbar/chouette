/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */
package mobi.chouette.core;


@SuppressWarnings("serial")
public class CloudRuntimeException extends ChouetteRuntimeException {
	private static final String PREFIX = "CLOUD";

	public CloudRuntimeException(String message) {
		super(message);
	}

	public CloudRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.certu.chouette.common.ChouetteRuntimeException#getPrefix()
	 */
	@Override
	public String getPrefix() {
		return PREFIX;
	}

	@Override
	public String getCode() {
		return PREFIX;
	}

}
