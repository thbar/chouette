package mobi.chouette.model.blueprint;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.field.FieldCallback;
import mobi.chouette.model.ConnectionLink;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ConnectionLinkTypeEnum;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@SuppressWarnings("deprecation")
@Blueprint(ConnectionLink.class)
public class ConnectionLinkBlueprint
{

   @Default
   FieldCallback objectId = new FieldCallback()
   {
      @Override
      public String get(Object model)
      {
         return "TEST:ConnectionLink:" + UUID.randomUUID();
      }

   };

   @Default
   String name = "Conection Link";

   @Default
   String comment = "Comment Connection Link";

   @Mapped
   StopArea startOfLink;

   @Mapped
   StopArea endOfLink;

   @Default
   BigDecimal linkDistance = new BigDecimal(2);

   @Default
   Duration defaultDuration = Duration.ofNanos(173335738);

   @Default
   Duration frequentTravellerDuration = Duration.ofNanos(173335738);

   @Default
   Duration occasionalTravellerDuration = Duration.ofNanos(173335738);

   @Default
   int objectVersion = 1;

   @Default
   ConnectionLinkTypeEnum linkType = ConnectionLinkTypeEnum.Mixed;

   @Default
   Duration mobilityRestrictedTravellerDuration = Duration.ofNanos(1908888);

   @Mapped
   boolean mobilityRestrictedSuitable = true;

}
