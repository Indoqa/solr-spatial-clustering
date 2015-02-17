# Indoqa solr-spatialclustering

This project offers a Distance-based spatial clustering search component for Apache Solr. 
It addresses the problem of reducing the amount of displayed markers on a map, described as [Spatial Clustering](https://wiki.apache.org/solr/SpatialClustering), 
using a [Distance-based](https://developers.google.com/maps/articles/toomanymarkers#distancebasedclustering) clustering algorithm based on [GVM](http://www.tomgibara.com/clustering/fast-spatial/).
``
The search component aggregates all possible search results to a maximum amount of pins and adds this information to the standard search result representation. Like faceting, it can be used to query 
for a paged result slice (eg. for a result list) and a geographic overview of ALL search result items (spatial clusters) at once. 

## Installation

### Requirements

  * Apache Solr 4.3+
  * Java 7+
  
### Build

  * Download the latest release
  * run "maven clean install"
  
### Deployment

  * Copy the plugin jar from 'target/solr-spatialclustering-{version}.jar' into the /lib directory of your solr core.
  * Copy all gvm dependency jars from 'target/lib/*.jar' into the /lib directory of your solr core.

## Configuration

### schema.xml

To enable spatial clustering, store the geo information (longitude and latitude) in your solr document:

```xml
<field name="latitude" type="sdouble" indexed="true" stored="true" />
<field name="longitude" type="sdouble" indexed="true" stored="true" />
```

### solrconfig.xml

Define the search component and map field names for id, longitude and latitude:

```xml
<searchComponent class="com.indoqa.solr.spatialclustering.SpatialClusteringComponent" name="spatial-clustering">
  <str name="fieldId">id</str>
  <str name="fieldLon">longitude</str>
  <str name="fieldLat">latitude</str>
</searchComponent>
```

After that, add the spatial component to your query component chain:

```xml
<requestHandler name="search" class="solr.SearchHandler" default="true">
  <arr name="last-components">
    <str>spatial-clustering</str>
  </arr>
</requestHandler>
```

## Usage

### Query Parameters

 * spatial-clustering=true -> Enabled spatial clustering
 * spatial-clustering.size=20 -> Optionally sets the maximum amount of clusters (=pins)

### Result

Similar to facets, the computed clusters are added to the search result after the requested documents. There are two types of
result pins:

  * "single": Represents a single document, including the id of the referenced document.
  * "cluster": Represents an aggregated pin covering more than one document, including the cluster size.  
  

```xml
<lst name="spatial-clustering">
  <lst name="pin">
    <str name="type">single</str>
    <int name="size">1</int>
    <double name="longitude">16.345518</double>
    <double name="latitude">48.285202</double>
    <string name="reference">document-2313</string>
  </lst>
  <lst name="pin">
    <str name="type">cluster</str>
    <int name="size">3</int>
    <double name="longitude">16.2461115932</double>
    <double name="latitude">48.20259082573333</double>
  </lst>
  ...
  ...
</lst>

```





 


