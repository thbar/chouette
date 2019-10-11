package mobi.chouette.exchange.netexprofile.exporter.writer;

import java.util.Collection;
import java.util.Iterator;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.NetexFragmentMode;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;

import org.apache.commons.collections.MapUtils;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.RoutePoint;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.rutebanken.netex.model.ServiceLink;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.*;

public class ServiceFrameWriter extends AbstractNetexWriter {

	public static void write(XMLStreamWriter writer, Context context, Network network, Marshaller marshaller) {
		String serviceFrameId = NetexProducerUtils.createUniqueId(context, SERVICE_FRAME);

		try {
			writer.writeStartElement(SERVICE_FRAME);
			writer.writeAttribute(VERSION, NETEX_DEFAULT_OBJECT_VERSION);
			writer.writeAttribute(ID, serviceFrameId);
			ElementsWriter.writeNetworkElement(writer, network, marshaller);
			writer.writeEndElement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void write(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, NetexFragmentMode fragmentMode,
							 Marshaller marshaller) {

		String serviceFrameId = NetexProducerUtils.createUniqueId(context, SERVICE_FRAME);

		try {
			writer.writeStartElement(SERVICE_FRAME);
			writer.writeAttribute(VERSION, NETEX_DEFAULT_OBJECT_VERSION);
			writer.writeAttribute(ID, serviceFrameId);

			if (fragmentMode.equals(NetexFragmentMode.LINE)) {
				ElementsWriter.writeRoutesElement(writer, exportableNetexData, marshaller);
				ElementsWriter.writeLinesElement(writer, exportableNetexData, marshaller);
				ElementsWriter.writeJourneyPatternsElement(writer, exportableNetexData, marshaller);
				ReusedConstructsWriter.writeNoticeAssignmentsElement(writer, exportableNetexData.getNoticeAssignmentsServiceFrame(), marshaller);
			} else { // shared data
				ElementsWriter.writeNetworks(writer, exportableNetexData, marshaller);
				ElementsWriter.writeRoutePointsElement(writer, exportableNetexData, marshaller);
				ElementsWriter.writeDestinationDisplaysElement(writer, exportableNetexData, marshaller);
				ElementsWriter.writeScheduledStopPointsElement(writer, exportableNetexData, marshaller);
				ElementsWriter.writeServiceLinkElements(writer, exportableNetexData, marshaller);
				ElementsWriter.writeStopAssignmentsElement(writer, exportableNetexData, marshaller);
				ElementsWriter.writeNoticesElement(writer, exportableNetexData.getSharedNotices().values(), marshaller);
			}

			writer.writeEndElement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
