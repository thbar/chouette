package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.NetexFragmentMode;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExportParameters;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import mobi.chouette.exchange.netexprofile.importer.validation.idfm.IDFMCalendarNetexProfileValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.idfm.IDFMCommonNetexProfileValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.idfm.IDFMLineNetexProfileValidator;
import org.rutebanken.netex.model.TypeOfFrameRefStructure;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.LocalDateTime;

import static mobi.chouette.common.Constant.CREATION_DATE;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.COMPOSITE_FRAME;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.DATA_OBJECTS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.FRAMES;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_CALENDRIER;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_COMMUN;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_HORAIRE;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_RESEAUX;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETEX_LIGNE;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.PARTICIPANT_REF;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.PUBLICATION_DELIVERY;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.PUBLICATION_TIMESTAMP;

public class PublicationDeliveryIDFMWriter extends AbstractNetexWriter {
    public static void write(Context context, XMLStreamWriter writer, ExportableData exportableData, ExportableNetexData exportableNetexData,
                             NetexFragmentMode fragmentMode, Marshaller marshaller) {

        LocalDateTime timestamp = (LocalDateTime) context.get(CREATION_DATE);
        String [] splitTimestampFormatted = formatter.format(timestamp).split("\\.");
        String timestampFormatted = splitTimestampFormatted[0];

        try {
            writer.writeStartElement(PUBLICATION_DELIVERY);
            writer.writeDefaultNamespace(Constant.NETEX_NAMESPACE);
            writer.writeNamespace("gis", Constant.OPENGIS_NAMESPACE);
            writer.writeNamespace("siri", Constant.SIRI_NAMESPACE);
            writer.writeAttribute(VERSION, getProfileVersion(fragmentMode));

            writeElement(writer, PUBLICATION_TIMESTAMP, timestampFormatted);

            NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);
            writeElement(writer, PARTICIPANT_REF, configuration.getDefaultCodespacePrefix());

            writeDataObjectsElement(context, writer, exportableData, exportableNetexData, timestampFormatted, fragmentMode, marshaller);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeDataObjectsElement(Context context, XMLStreamWriter writer, ExportableData exportableData, ExportableNetexData exportableNetexData,
                                                String timestamp, NetexFragmentMode fragmentMode, Marshaller marshaller) {
        try {
            writer.writeStartElement(DATA_OBJECTS);
            if(fragmentMode.equals(NetexFragmentMode.LINE)){
                writeCompositeFrameElement(context, writer, exportableData, exportableNetexData, timestamp, fragmentMode, marshaller);
            }
            else{
                writeGeneralFrameElement(context, writer, exportableNetexData, timestamp, fragmentMode, marshaller);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getProfileVersion(NetexFragmentMode fragmentMode){
        if(fragmentMode.equals(NetexFragmentMode.LINE)){
            return IDFMLineNetexProfileValidator.NETEX_FRANCE_RESEAU_PROFILE;
        }else if(fragmentMode.equals(NetexFragmentMode.CALENDAR)){
            return IDFMCalendarNetexProfileValidator.NETEX_FRANCE_CALENDAR_PROFILE;
        }else if(fragmentMode.equals(NetexFragmentMode.COMMON)){
            return IDFMCommonNetexProfileValidator.NETEX_FRANCE_COMMON_PROFILE;
        }else {
            return NETEX_PROFILE_VERSION;
        }

    }

    private static void writeGeneralFrameElement(Context context, XMLStreamWriter writer, ExportableNetexData exportableNetexData, String timestamp, NetexFragmentMode fragmentMode, Marshaller marshaller) {

        if(fragmentMode.equals(NetexFragmentMode.LINE)){
            GeneralFrameIDFMWriter.write(writer, context, exportableNetexData, fragmentMode, marshaller, timestamp, NETEX_LIGNE);
            GeneralFrameIDFMWriter.write(writer, context, exportableNetexData, fragmentMode, marshaller, timestamp, NETEX_HORAIRE);
        }

        if(fragmentMode.equals(NetexFragmentMode.CALENDAR)){
            GeneralFrameIDFMWriter.write(writer, context, exportableNetexData, fragmentMode, marshaller, timestamp, NETEX_CALENDRIER);
        }

        if(fragmentMode.equals(NetexFragmentMode.COMMON)){
            GeneralFrameIDFMWriter.write(writer, context, exportableNetexData, fragmentMode, marshaller, timestamp, NETEX_COMMUN);
        }
    }

    private static void writeCompositeFrameElement(Context context, XMLStreamWriter writer, ExportableData exportableData,
                                                   ExportableNetexData exportableNetexData, String timestamp, NetexFragmentMode fragmentMode, Marshaller marshaller) {
        mobi.chouette.model.Line line = exportableData.getLine();
        String compositeFrameId = NetexProducerUtils.createUniqueCompositeFrameInLineId(context, COMPOSITE_FRAME, NETEX_RESEAUX);

        try {
            writer.writeStartElement(COMPOSITE_FRAME);

            writer.writeAttribute(VERSION, NETEX_DEFAULT_OBJECT_VERSION);
            writer.writeAttribute(ID, compositeFrameId);

            writeElement(writer, NAME, line.getName());

            TypeOfFrameRefStructure typeOfFrameRefStructure = new TypeOfFrameRefStructure();
            typeOfFrameRefStructure.withRef(PARTICIPANT_REF_CONTENT + ":TypeOfFrame:" + NETEX_RESEAUX + ":");
            typeOfFrameRefStructure.withValue("version=\"1.1:FR-NETEX_RESEAUX-2.2\"");
            marshaller.marshal(netexFactory.createTypeOfFrameRef(typeOfFrameRefStructure), writer);

            writer.writeStartElement(FRAMES);

            writeGeneralFrameElement(context, writer, exportableNetexData, timestamp, fragmentMode, marshaller);

            writer.writeEndElement();
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
