package mobi.chouette.exchange.stopplace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.nashorn.internal.objects.annotations.Setter;
import lombok.Getter;
import mobi.chouette.model.StopArea;

public class StopAreaUpdateContext {

	@Getter
	private Set<String> inactiveStopAreaIds = new HashSet<>();
	@Getter
	private Set<StopArea> activeStopAreas = new HashSet<>();
	@Getter
	private Map<String, Set<String>> mergedQuays = new HashMap<>();
	@Getter
	private Set<String> impactedSchemas = new HashSet<>();

	@Getter
	private Map<String, List<String>> impactedStopAreasBySchema = new HashMap<>();



	public int getChangedStopCount() {
		return getActiveStopAreas().size() + getInactiveStopAreaIds().size();
	}

}
