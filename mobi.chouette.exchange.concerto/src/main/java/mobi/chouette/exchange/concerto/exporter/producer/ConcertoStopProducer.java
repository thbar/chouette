/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.concerto.exporter.producer;

import mobi.chouette.exchange.concerto.model.ConcertoStopArea;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporterInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data pour stop_area
 */
public class ConcertoStopProducer extends AbstractProducer {

	public ConcertoStopProducer(ConcertoExporterInterface exporter) {
		super(exporter);
	}

	public void save(List<ConcertoStopArea> concertoStopAreas) {
		Map<UUID, UUID> concertoStopAreasToDelete = new HashMap<>();
		for (ConcertoStopArea concertoStopArea : concertoStopAreas) {
			for (ConcertoStopArea concertoStopArea1 : concertoStopAreas) {

				if (!concertoStopArea.getUuid().equals(concertoStopArea1.getUuid()) &&
						concertoStopArea.getObjectId().equals(concertoStopArea1.getObjectId()) &&
							concertoStopArea.getDate().equals(concertoStopArea1.getDate()) &&
									!concertoStopAreasToDelete.containsKey(concertoStopArea1.getUuid())) {

					UUID[] lineIdArray;
					List<UUID> uuids = new ArrayList<>(Arrays.asList(concertoStopArea.getLines()));
					List<UUID> uuids1 = new ArrayList<>(Arrays.asList(concertoStopArea1.getLines()));
					uuids.addAll(uuids1);
					lineIdArray = new UUID[uuids.size()];
					lineIdArray = uuids.toArray(lineIdArray);
					concertoStopArea.setLines(lineIdArray);
					concertoStopAreasToDelete.put(concertoStopArea.getUuid(), concertoStopArea1.getUuid());
				}
			}
		}

		concertoStopAreas.removeIf(concertoStopArea -> concertoStopAreasToDelete.containsValue(concertoStopArea.getUuid()));

		for (ConcertoStopArea concertoStopArea : concertoStopAreas) {
			try {
				getExporter().getStopAreaExporter().export(concertoStopArea);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}