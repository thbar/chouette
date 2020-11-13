package mobi.chouette.exchange.gtfs.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.awt.*;
import java.io.Serializable;
import java.net.URL;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsRoute extends GtfsObject implements Serializable
{

   private static final long serialVersionUID = 1L;

   @Getter
   @Setter
   private String routeId;

   @Getter
   @Setter
   private String agencyId;

   @Getter
   @Setter
   private String routeShortName;

   @Getter
   @Setter
   private String routeLongName;

   @Getter
   @Setter
   private String routeDesc;

   @Getter
   @Setter
   private RouteTypeEnum routeType;

   @Getter
   @Setter
   private URL routeUrl;

   @Getter
   @Setter
   private Color routeColor;

   @Getter
   @Setter
   private Color routeTextColor;

   @Getter
   @Setter
   private Integer position;

   public GtfsRoute(GtfsRoute bean) {
	   this(bean.getRouteId(), bean.getAgencyId(), bean.getRouteShortName(), bean.getRouteLongName(), bean.getRouteDesc(), bean.getRouteType(), bean.getRouteUrl(), bean.getRouteColor(), bean.getRouteTextColor(), bean.getPosition());
	   this.setId(bean.getId());
   }
   
//   public enum RouteType implements Serializable
//   {
//      Tram, Subway, Rail, Bus, Ferry, Cable, Gondola, Funicular;
//
//   }

   // @Override
   // public String toString()
   // {
   // return id + ":" + RouteExporter.CONVERTER.to(new Context(),this);
   // }
}
