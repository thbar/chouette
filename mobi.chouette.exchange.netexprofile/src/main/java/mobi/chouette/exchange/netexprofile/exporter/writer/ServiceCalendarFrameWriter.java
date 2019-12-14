package mobi.chouette.exchange.netexprofile.exporter.writer;

import java.math.BigInteger;
import java.util.stream.Collectors;

import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;

import org.apache.commons.collections.MapUtils;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.*;

public class ServiceCalendarFrameWriter extends AbstractNetexWriter {

    public static void write(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller) {

        try {
            writeDayTypesElement(writer, exportableNetexData,marshaller);

            if (MapUtils.isNotEmpty(exportableNetexData.getSharedOperatingPeriods())) {
                writeOperatingPeriodsElement(writer, exportableNetexData,marshaller);
            }

            writeDayTypeAssignmentsElement(writer, exportableNetexData,marshaller);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeDayTypesElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            for (DayType dayType : exportableData.getSharedDayTypes().values()) {
                marshaller.marshal(netexFactory.createDayType(dayType), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeDayTypeAssignmentsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            for (DayTypeAssignment dayTypeAssignment : exportableData.getSharedDayTypeAssignments().stream().sorted(new DayTypeAssignmentExportComparator()).collect(Collectors.toList())) {
                marshaller.marshal(netexFactory.createDayTypeAssignment(dayTypeAssignment), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeOperatingPeriodsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (OperatingPeriod operatingPeriod : exportableNetexData.getSharedOperatingPeriods().values()) {
                marshaller.marshal(netexFactory.createOperatingPeriod(operatingPeriod), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
