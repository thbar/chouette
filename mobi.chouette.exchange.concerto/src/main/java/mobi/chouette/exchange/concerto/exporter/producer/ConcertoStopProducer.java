/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.concerto.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.concerto.exporter.MappingLineUUID;
import mobi.chouette.exchange.concerto.model.ConcertoStopArea;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporterInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static mobi.chouette.common.Constant.MAPPING_LINE_UUID;

/**
 * Data pour stop_area
 */
public class ConcertoStopProducer extends AbstractProducer {

	public ConcertoStopProducer(ConcertoExporterInterface exporter) {
		super(exporter);
	}

	public void save(Context context, List<ConcertoStopArea> concertoStopAreas) {
		Map<UUID, UUID> concertoStopAreasToDelete = new HashMap<>();
		List<MappingLineUUID> mappingLineUUIDList = (List<MappingLineUUID>)context.get(MAPPING_LINE_UUID);

		for (ConcertoStopArea concertoStopArea : concertoStopAreas) {
			for (ConcertoStopArea concertoStopArea1 : concertoStopAreas) {

				if (!concertoStopArea.getUuid().equals(concertoStopArea1.getUuid()) &&
						concertoStopArea.getObjectId().equals(concertoStopArea1.getObjectId()) &&
							concertoStopArea.getDate().equals(concertoStopArea1.getDate()) &&
									!concertoStopAreasToDelete.containsValue(concertoStopArea1.getUuid())) {

					UUID[] lineIdArray;
					List<UUID> uuids = new ArrayList<>(Arrays.asList(concertoStopArea.getLines()));
					List<UUID> uuids1 = new ArrayList<>(Arrays.asList(concertoStopArea1.getLines()));
					uuids.addAll(uuids1);

					uuids.removeIf(uuid -> mappingLineUUIDList.stream().noneMatch(mappingLineUUID -> mappingLineUUID.getUuid().equals(uuid)));

					lineIdArray = new UUID[uuids.size()];
					lineIdArray = uuids.toArray(lineIdArray);
					concertoStopArea.setLines(lineIdArray);
					concertoStopAreasToDelete.put(concertoStopArea1.getUuid(), concertoStopArea.getUuid());
				}
			}
		}

		concertoStopAreas.removeIf(concertoStopArea -> concertoStopAreasToDelete.containsKey(concertoStopArea.getUuid()));

		for (ConcertoStopArea concertoStopArea : concertoStopAreas) {
			try {
				getExporter().getStopAreaExporter().export(concertoStopArea);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}