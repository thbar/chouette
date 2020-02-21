package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.NetexFragmentMode;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.exporter.writer.AbstractNetexWriter.VERSION;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.GENERAL_FRAME;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_HORAIRE;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_STRUCTURE;

public class GeneralFrameIDFMWriter {

    public static void write(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, NetexFragmentMode fragmentMode,
                             Marshaller marshaller, String timestamp, String typeNetex) {
        try {

            writer.writeStartElement(GENERAL_FRAME);
            writer.writeAttribute(VERSION, NETEX_DEFAULT_OBJECT_VERSION);

            exportableNetexData.getServiceJourneys().stream()
                    .flatMap(sj -> sj.getDayTypes().getDayTypeRef().stream())
                    .filter(dtr -> dtr.getValue().getVersion() != null)
                    .forEach(dtr -> {
                        dtr.getValue().setValue("version=\"" + dtr.getValue().getVersion() + "\"");
                        dtr.getValue().setVersion(null);
                    });

            if (typeNetex.equals(NETEX_STRUCTURE)) {
                NetexStructureWriter.writer(writer, context, exportableNetexData, marshaller, timestamp, typeNetex);
            }

            if (typeNetex.equals(NETEX_HORAIRE)) {
                NetexHoraireWriter.writer(writer, context, exportableNetexData, marshaller, timestamp, typeNetex);
            }

            if (fragmentMode.equals(NetexFragmentMode.CALENDAR)) {
                NetexCalendrierWriter.writer(writer, context, exportableNetexData, marshaller, timestamp, typeNetex);
            }

            if (fragmentMode.equals(NetexFragmentMode.COMMON)) {
                NetexCommunWriter.writer(writer, context, exportableNetexData, marshaller, timestamp,typeNetex);
            }

            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

}
