package mobi.chouette.exchange.gtfs.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ConnectionLinkDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.LineRegisterCommand;
import mobi.chouette.model.ConnectionLink;
import mobi.chouette.model.Line;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.StringUtils;


import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@Stateless(name = ConnectionLinkPersisterCommand.COMMAND)
@Log4j
public class ConnectionLinkPersisterCommand implements Command, Constant {

    @EJB
    private ConnectionLinkDAO connectionLinkDAO;

    @EJB
    private StopAreaDAO stopAreaDAO;

    public static final String COMMAND = "ConnectionLinkPersisterCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        //List<ConnectionLink> connectionLinkList = (List<ConnectionLink>) context.get(CONNECTION_LINK_LIST);
        //connectionLinkList.forEach(connectionLinkDAO::create);

        Referential referential = (Referential) context.get(REFERENTIAL);
        Map<String,String> fileToReferentialStopIdMap =  (Map<String,String>) context.get(FILE_TO_REFERENTIAL_STOP_ID_MAP);

        if (fileToReferentialStopIdMap == null)
            return SUCCESS;

        referential.getSharedConnectionLinks().forEach((key,connectionLink)->{

            String originalStartID = connectionLink.getStartOfLink().getObjectId();
            String originalEndID = connectionLink.getEndOfLink().getObjectId();

            String referentialStartId = fileToReferentialStopIdMap.get(originalStartID);
            String referentialEndId = fileToReferentialStopIdMap.get(originalEndID);

            createConnectionLink(connectionLink,referentialStartId,referentialEndId);

        });

        return SUCCESS;
    }

    private void createConnectionLink(ConnectionLink connectionLink, String startId, String endId){
        if (StringUtils.isEmpty(startId) || StringUtils.isEmpty(endId))
            return;

        StopArea startArea = stopAreaDAO.findByObjectId(startId);
        StopArea endArea = stopAreaDAO.findByObjectId(endId);

        if (startArea == null || endArea == null)
            return;

        ConnectionLink existingConnectionLink = connectionLinkDAO.findByObjectId(connectionLink.getObjectId());

        if (existingConnectionLink == null){
            //new connection link is created in DB
            connectionLink.setStartOfLink(startArea);
            connectionLink.setEndOfLink(endArea);
            connectionLinkDAO.create(connectionLink);
        }else{
            //existing connection link is updated
            existingConnectionLink.setStartOfLink(startArea);
            existingConnectionLink.setEndOfLink(endArea);
            existingConnectionLink.setComment(connectionLink.getComment());
            existingConnectionLink.setDefaultDuration(connectionLink.getDefaultDuration());
            existingConnectionLink.setFrequentTravellerDuration(connectionLink.getFrequentTravellerDuration());
            existingConnectionLink.setIntUserNeeds(connectionLink.getIntUserNeeds());
            existingConnectionLink.setLiftAvailable(connectionLink.getLiftAvailable());
            existingConnectionLink.setLinkDistance(connectionLink.getLinkDistance());
            existingConnectionLink.setLinkType(connectionLink.getLinkType());
            existingConnectionLink.setMobilityRestrictedSuitable(connectionLink.getMobilityRestrictedSuitable());
            existingConnectionLink.setMobilityRestrictedTravellerDuration(connectionLink.getMobilityRestrictedTravellerDuration());
            existingConnectionLink.setName(connectionLink.getName());
            existingConnectionLink.setOccasionalTravellerDuration(connectionLink.getOccasionalTravellerDuration());
            existingConnectionLink.setObjectVersion(connectionLink.getObjectVersion());
        }
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.gtfs/" + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                // try another way on test context
                String name = "java:module/" + COMMAND;
                try {
                    result = (Command) context.lookup(name);
                } catch (NamingException e1) {
                    log.error(e);
                }
            }
            return result;
        }
    }


    static {
        CommandFactory.factories.put(ConnectionLinkPersisterCommand.class.getName(), new ConnectionLinkPersisterCommand.DefaultCommandFactory());
    }

}
