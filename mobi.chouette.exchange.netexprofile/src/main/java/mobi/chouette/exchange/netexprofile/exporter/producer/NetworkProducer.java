package mobi.chouette.exchange.netexprofile.exporter.producer;

import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.PrivateCodeStructure;

import java.time.OffsetDateTime;

import static mobi.chouette.exchange.netexprofile.exporter.ModelTranslator.netexId;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.NETWORK_KEY;

public class NetworkProducer extends NetexProducer implements NetexEntityProducer<org.rutebanken.netex.model.Network, mobi.chouette.model.Network> {

    @Override
    public org.rutebanken.netex.model.Network produce(mobi.chouette.model.Network neptuneNetwork) {
        org.rutebanken.netex.model.Network netexNetwork = netexFactory.createNetwork();
        netexNetwork.setVersion(neptuneNetwork.getObjectVersion() > 0 ? String.valueOf(neptuneNetwork.getObjectVersion()) : NETEX_DATA_OJBECT_VERSION);

        String networkId = netexId(neptuneNetwork.objectIdPrefix(), NETWORK_KEY, neptuneNetwork.objectIdSuffix());
        netexNetwork.setId(networkId);

        if (isSet(neptuneNetwork.getVersionDate())) {
            OffsetDateTime changedDateTime = NetexProducerUtils.toOffsetDateTime(neptuneNetwork.getVersionDate());
            netexNetwork.setChanged(changedDateTime);
        }

        if (isSet(neptuneNetwork.getComment())) {
            KeyValueStructure keyValueStruct = netexFactory.createKeyValueStructure()
                    .withKey("Comment")
                    .withValue(neptuneNetwork.getComment());
            netexNetwork.setKeyList(netexFactory.createKeyListStructure().withKeyValue(keyValueStruct));
        }

        if (isSet(neptuneNetwork.getName())) {
            netexNetwork.setName(getMultilingualString(neptuneNetwork.getName()));
        }

        if (isSet(neptuneNetwork.getDescription())) {
            netexNetwork.setDescription(getMultilingualString(neptuneNetwork.getDescription()));
        }

        if (isSet(neptuneNetwork.getRegistrationNumber())) {
            PrivateCodeStructure privateCodeStruct = netexFactory.createPrivateCodeStructure();
            privateCodeStruct.setValue(neptuneNetwork.getRegistrationNumber());
            netexNetwork.setPrivateCode(privateCodeStruct);
        }

        return netexNetwork;
    }
}
