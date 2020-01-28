/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.concerto.exporter.producer;

import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import mobi.chouette.exchange.concerto.model.ConcertoStopArea;
import mobi.chouette.exchange.concerto.model.StopAreaTypeEnum;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporterInterface;
import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.model.StopArea;
import org.joda.time.LocalDate;

import java.util.UUID;

/**
 * Data pour stop_area
 */
public class ConcertoStopProducer extends AbstractProducer
{
	private final String TYPE = "stop_area";

	ConcertoStopArea stop = new ConcertoStopArea();

	public ConcertoStopProducer(ConcertoExporterInterface exporter)
	{
		super(exporter);
	}

	public UUID save(StopArea neptuneObject, StopArea parent, LocalDate startDate, LocalDate endDate, ConcertoObjectId objectId, UUID[] lines, StopAreaTypeEnum stopAreaType)
	{
		UUID uuid;
		if(neptuneObject.getUuid() != null) {
			uuid = neptuneObject.getUuid();
		} else {
			uuid = UUID.randomUUID();
		}

		for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1))
		{
			save(neptuneObject, parent, date, uuid, objectId, lines, stopAreaType);
    	}
		return uuid;
	}

	private void save(StopArea neptuneObject, StopArea referent, LocalDate date, UUID uuid, ConcertoObjectId objectId, UUID[] lines, StopAreaTypeEnum stopAreaType){
		stop.setType(TYPE);
		stop.setUuid(uuid);
		if(stopAreaType == StopAreaTypeEnum.ZDEP && referent != null){
			stop.setReferent_uuid(referent.getUuid());
		} else {
			stop.setReferent_uuid(null);
		}

		// If name is empty, try to use parent name
		String name = neptuneObject.getName();
		if (name == null && neptuneObject.getParent() != null) {
			name = neptuneObject.getParent().getName();
		}
		if(name == null) {
			return;
		}
		stop.setDate(date);
		stop.setName(name);
		stop.setObjectId(objectId);

		MappingHastusZdep mappingHastusZdep = neptuneObject.getMappingHastusZdep();
		// @todo SCH PA NON IDFM ? A check avec Vincent
		if(mappingHastusZdep == null) return;
		stop.setZdep(mappingHastusZdep.getZdep());
		stop.setZder(mappingHastusZdep.getZder());
		stop.setZdlr(mappingHastusZdep.getZdlr());
		stop.setLines(lines);
		stop.setAttributes("{}");
		stop.setReferences("{}");
		stop.setCollectedAlways(true);
		stop.setCollectChildren(false);
		stop.setCollectGeneralMessages(true);
		try
		{
			getExporter().getStopAreaExporter().export(stop);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
