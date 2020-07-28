package mobi.chouette.exchange.gtfs.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.gtfs.model.GtfsFeedInfo;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;

import java.net.MalformedURLException;
import java.net.URL;

@Log4j
public class GtfsFeedInfoProducer extends AbstractProducer {
    public GtfsFeedInfoProducer(GtfsExporterInterface exporter) {
        super(exporter);
    }

    private GtfsFeedInfo feedInfo = new GtfsFeedInfo();

    public boolean save(GtfsFeedInfo gtfsFeedInfo) throws MalformedURLException {

        if (gtfsFeedInfo != null) {
            feedInfo = gtfsFeedInfo;
        }

        if (feedInfo.getFeedPublisherName() == null) {
            feedInfo.setFeedPublisherName("MOSAIC");
        }

        if (feedInfo.getFeedPublisherUrl() == null) {
            feedInfo.setFeedPublisherUrl(new URL("https://www.ratpdev.com"));
        }

        if (feedInfo.getFeedLang() == null) {
            feedInfo.setFeedLang("FR");
        }

        try {
            getExporter().getFeedInfoExporter().export(feedInfo);
        } catch (Exception e) {
            log.warn("export failed for feed info ", e);
            return false;
        }

        return true;
    }
}
