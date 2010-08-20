Chouette.Map.init = function(){
  Chouette.Map.initMap();

  // Add button for barycentre
  var barycentre = new OpenLayers.Control.Button({
    displayClass: "mapBarycentre",
    trigger: Chouette.Map.barycentreStopPlaceMarker
  });
  var panel = new OpenLayers.Control.Panel();
  panel.addControls([barycentre]);
  var pixel = new OpenLayers.Pixel(15, 260);
  Chouette.Map.map.addControl(panel, pixel);
  
  // edit marker layer
  var editMarkerLayer = Chouette.Map.createEditMarkerLayer();

  //show marker layer
  var showMarkerLayer = Chouette.Map.createShowMarkerLayer();

  Chouette.Map.map.addLayers([editMarkerLayer,showMarkerLayer]);
  Chouette.Map.map.zoomToMaxExtent();

  Chouette.Map.initEditMarkerLayer("stoparea");

  var stopPlaceId = $("stoparea_idPositionGeographique").value ;
  if(stopPlaceId != null){
    Chouette.Map.initShowMarkerLayer("../json/JSONStopPlace?stopPlaceId="+stopPlaceId);
  }
  Chouette.Map.updateEditMarker();
};

Chouette.Map.barycentreStopPlaceMarker = function(){
  var showMarkerLayer = Chouette.Map.map.getLayersByName("Show Marker Layer")[0];
  var childrenPoints = showMarkerLayer.features.collect(function(feature){
    return feature.geometry;
  });

  if(childrenPoints.length > 1)
  {
    var point = Chouette.Map.barycentre(childrenPoints).transform(Chouette.Map.geoportalProjection, Chouette.Map.lambertProjection);

    $("stoparea_x").value=point.lon;
    $("stoparea_y").value=point.lat;

    Chouette.Map.updateLatLonFieldsCoordsFromXY();
    Chouette.Map.updateEditMarker();
  }
}

window.onload = Chouette.Map.init;