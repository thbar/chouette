package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.model.Footnote;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.NoticeAssignments_RelStructure;
import org.rutebanken.netex.model.NoticeRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TypeOfNoticeRefStructure;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.Collection;


import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NOTICE_ASSIGNMENT;

public class NoticeIDFMProducer {

    public static void addNoticeAndNoticeAssignments(Context context, ExportableNetexData exportableNetexData, ServiceJourney serviceJourney,
                                                     Collection<Footnote> footnotes) {
        for (Footnote footnote : footnotes) {
            Notice notice = netexFactory.createNotice();
            NetexProducerUtils.populateIdAndVersionIDFM(footnote, notice);

            if (!exportableNetexData.getSharedNotices().containsKey(notice.getId())) {
                notice.setText(ConversionUtil.getMultiLingualString(footnote.getLabel()));
                notice.setPublicCode(footnote.getCode());
                TypeOfNoticeRefStructure typeOfNoticeRefStructure = netexFactory.createTypeOfNoticeRefStructure();
                typeOfNoticeRefStructure.setRef("ServiceJourneyNotice");
                notice.setTypeOfNoticeRef(typeOfNoticeRefStructure);

                exportableNetexData.getSharedNotices().put(notice.getId(), notice);
            }
            NoticeRefStructure noticeRefStruct = netexFactory.createNoticeRefStructure();
            noticeRefStruct.setRef(notice.getId());
            noticeRefStruct.setValue("version=\"any\"");

            String noticeAssignmentId = NetexProducerUtils.createUniqueIDFMId(context, NOTICE_ASSIGNMENT);
            NoticeAssignment noticeAssignment = new NoticeAssignment();
            noticeAssignment.setId(noticeAssignmentId);
            noticeAssignment.setVersion("any");
            noticeAssignment.setOrder(BigInteger.valueOf(0));
            noticeAssignment.setNoticeRef(noticeRefStruct);

            JAXBElement<NoticeAssignment> noticeAssignmentJAXBElement = netexFactory.createNoticeAssignment(noticeAssignment);
            NoticeAssignments_RelStructure noticeAssignments_relStructure = netexFactory.createNoticeAssignments_RelStructure();
            noticeAssignments_relStructure.withNoticeAssignment_OrNoticeAssignmentView(noticeAssignmentJAXBElement);

            serviceJourney.setNoticeAssignments(noticeAssignments_relStructure);
        }

    }
}
