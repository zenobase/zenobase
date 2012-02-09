package org.elasticsearch.search.facet.geocluster;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.mapper.geo.GeoPoint;
import org.elasticsearch.index.search.geo.GeoDistance;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

public class ClusterTest {

	List<String> cityMarkers = Lists.newArrayList("small_red", "small_yellow", "small_green", "small_blue", "small_purple", "measle_brown", "measle_white", "measle_turquoise");
	List<String> clusterMarkers = Lists.newArrayList("red_blank", "ylw_blank", "grn_blank", "blu_blank", "pink_blank", "orange_blank", "wht_blank", "ltblu_blank");

	private Map<GeoPoint, String> loadCities() throws IOException {
		Map<GeoPoint, String> cities = Maps.newLinkedHashMap();
		for (String line : Files.readLines(new File("data/cities.txt"), Charsets.US_ASCII)) {
			String[] tokens = line.split("\t");
			double lat = Double.parseDouble(tokens[0]);
			double lon = -Double.parseDouble(tokens[1]);
			cities.put(new GeoPoint(lat, lon), tokens[2]);
		}
		return cities;
	}

	@Test
	public void test() throws IOException {
		Map<GeoPoint, String> cities = loadCities();
		Map<GeoPoint, GeoCluster> clusters = Maps.newLinkedHashMap();
		GeoClusterer clusterer = new GeoClusterer(100.0, DistanceUnit.KILOMETERS);
		for (GeoPoint point : cities.keySet()) {
			clusters.put(point, clusterer.add(point));
		}

		// System.out.println("var data = [");		
		// for (GeoPoint point : cities.keySet()) {
		//	System.out.printf("{ \"latitude\" : %.4f, \"longitude\" : %.4f, \"label\" : \"%s\" },\n", point.getLat(), point.getLon(), cities.get(point));
		// }
		System.out.println("]");		

		System.out.printf("point\tcluster\ticon\n");
		for (GeoPoint point : cities.keySet()) {
			System.out.printf("%.4f,%.4f\t%h\t%s\n", point.getLat(), point.getLon(), clusters.get(point).hashCode(), cityMarkers.get(clusters.get(point).hashCode() % cityMarkers.size()));
		}
		for (GeoCluster cluster : clusterer.getClusters()) {
			System.out.printf("%.4f,%.4f\t%h\t%s\n", cluster.center().getLat(), cluster.center().getLon(), cluster.hashCode(), clusterMarkers.get(cluster.hashCode() % clusterMarkers.size()));
		}
	}

	static String toString(GeoPoint point){
		return point.getLat() + ","+ point.getLon();
	}
	
	static class GeoClusterer {
		
		private final double maxDistance;
		private final List<GeoCluster> clusters = Lists.newArrayList();
		
		public GeoClusterer(double maxDistance, DistanceUnit unit) {
			this.maxDistance = unit.convert(maxDistance, unit, DistanceUnit.KILOMETERS);
		}
		
		public GeoCluster add(GeoPoint point) {
			GeoCluster cluster = null;
			double distance = Double.MAX_VALUE;
			for (GeoCluster c : clusters) {
				double d = distance(c.center(), point);
				if (d < distance && d <= maxDistance) {
					d = distance;
					cluster = c;
				}
			}
			if (cluster == null) {
				cluster = new GeoCluster();
				clusters.add(cluster);
			}
			cluster.add(point);
			return cluster;
		}
		
		private double distance(GeoPoint left, GeoPoint right) {
			return GeoDistance.ARC.calculate(left.getLat(), left.getLon(),
					right.getLat(), right.getLon(), DistanceUnit.KILOMETERS);
		}
		
		public List<GeoCluster> getClusters() {
			return clusters;
		}
	}
	
	static class GeoCluster {
		
		private int size;
		private GeoPoint center;
		
		public void add(GeoPoint point) {
			++size;
			if (center == null) {
				center = point;
			}
			else {
				double lat = (center.getLat() * (size - 1) + point.getLat()) / size;
				double lon = (center.getLon() * (size - 1) + point.getLon()) / size;
				center = new GeoPoint(lat, lon);
			}
		}
		
		public int size() {
			return size;
		}
		
		public GeoPoint center() {
			return center;
		}
	}
}
