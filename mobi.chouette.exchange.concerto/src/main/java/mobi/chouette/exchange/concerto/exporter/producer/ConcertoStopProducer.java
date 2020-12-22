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

import static mobi.chouette.exchange.concerto.model.StopAreaTypeEnum.ZDEP;
import static mobi.chouette.exchange.concerto.model.StopAreaTypeEnum.ZDER;
import static mobi.chouette.exchange.concerto.model.StopAreaTypeEnum.ZDLR;

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

	public UUID save(StopArea neptuneObject, StopArea parent, StopArea referent, LocalDate startDate, LocalDate endDate, String objectId, UUID[] lines, StopAreaTypeEnum stopAreaType)
	{
		UUID uuid;
		if(neptuneObject.getUuid() != null) {
			uuid = neptuneObject.getUuid();
		} else {
			uuid = UUID.randomUUID();
		}
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			save(neptuneObject, parent, referent, date, uuid, objectId, lines, stopAreaType);
		}

		return uuid;
	}

	private void save(StopArea neptuneObject, StopArea parent, StopArea referent, LocalDate date, UUID uuid, String objectId, UUID[] lines, StopAreaTypeEnum stopAreaType){
		switch(stopAreaType) {
			case ZDEP:
				break;
			case ZDER:
				if(neptuneObject.getMappingHastusZdep() == null || neptuneObject.getMappingHastusZdep().getZder() == null)
					return;
				break;
			case ZDLR:
				if(neptuneObject.getMappingHastusZdep() == null || neptuneObject.getMappingHastusZdep().getZdlr() == null)
					return;
				break;
			default:
				return;
		}

		stop.setType(TYPE);
		stop.setUuid(uuid);
		if(stopAreaType == ZDEP && referent != null){
			stop.setReferent_uuid(referent.getUuid());
		} else {
			stop.setReferent_uuid(null);
		}

		if(stopAreaType == ZDER && parent != null){
			stop.setParent_uuid(parent.getUuid());
		} else {
			stop.setParent_uuid(null);
		}

		// If name is empty, try to use parent name
		String name = neptuneObject.getName();
		if (stopAreaType == ZDLR && neptuneObject.getParent() != null) {
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
		stop.setAttributes("{}");
		stop.setReferences("{}");
		stop.setCollectedAlways(true);
		if(stopAreaType == ZDLR) {
			stop.setLines(new UUID[0]);
			stop.setCollectChildren(true);
		} else {
			stop.setLines(lines);
			stop.setCollectChildren(false);
		}
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
