package mobi.chouette.common.chain;

import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;

import javax.ejb.Local;

@Local
public interface Command extends Constant {

	boolean execute(Context context) throws Exception;
}
