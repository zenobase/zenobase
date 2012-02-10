package org.elasticsearch.search.facet.geocluster;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.mapper.geo.GeoPoint;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class GeoClusterTool {

	public static void main(String[] args) throws IOException {
		List<GeoPoint> points = new PointParser().parse(new File("data/cities.txt"));
		PointTracker tracker = new PointTracker();
		List<GeoCluster> clusters = new GeoClusterBuilder(500.0, DistanceUnit.KILOMETERS).addListener(tracker).addAll(points).build();
		new GeoClusterReducer(250.0, DistanceUnit.KILOMETERS).addListener(tracker).reduce(clusters);
		new PointPrinter(System.out).print(tracker.asMap());
	}

	static class PointParser {
		
		private List<GeoPoint> parse(File source) throws IOException {
			List<GeoPoint> points = Lists.newArrayList();
			for (String line : Files.readLines(source, Charsets.US_ASCII)) {
				String[] tokens = line.split("\t");
				double lat = Double.parseDouble(tokens[0]);
				double lon = -Double.parseDouble(tokens[1]);
				points.add(new GeoPoint(lat, lon));
			}
			return points;
		}
	}

	static class PointTracker implements GeoClusterBuilder.Listener, GeoClusterReducer.Listener {

		private final Multimap<GeoCluster, GeoPoint> points = ArrayListMultimap.create();

		public void added(GeoPoint point, GeoCluster cluster) {
			points.put(cluster, point);
		}

		public void merged(GeoCluster source, GeoCluster target) {
			points.putAll(target, points.removeAll(source));
		}

		public Map<GeoPoint, GeoCluster> asMap() {
			return invertFrom(points);
		}

		private static <K, V> Map<V, K> invertFrom(Multimap<K, V> multimap) {
			Map<V, K> map = Maps.newHashMap();
			for (K key : multimap.keySet()) {
				for (V value : multimap.get(key)) {
					map.put(value, key);
				}
			}
			return map;
		}
	}

	static class PointPrinter {

		private final ImmutableList<String> pointMarkers = ImmutableList.of(
			"small_red", "small_yellow", "small_green", "small_blue", 
			"small_purple", "measle_brown", "measle_white", "measle_turquoise");

		private final ImmutableList<String> clusterMarkers = ImmutableList.of(
			"red_blank", "ylw_blank", "grn_blank", "blu_blank", 
			"pink_blank", "orange_blank", "wht_blank", "ltblu_blank");

		private final String format = "%.4f,%.4f\t%h\t%s\n";
		private final PrintStream out;

		public PointPrinter(PrintStream out) {
			this.out = out;
		}

		public void print(Map<GeoPoint, GeoCluster> points) {
			out.printf("point\tcluster\tmarker\n");
			for (Map.Entry<GeoPoint, GeoCluster> entry : points.entrySet()) {
				out.printf(format, entry.getKey().getLat(), entry.getKey().getLon(), entry.getValue().hashCode(), pointMarker(entry.getValue()));
			}
			for (GeoCluster cluster : Sets.newHashSet(points.values())) {
				out.printf(format, cluster.center().getLat(), cluster.center().getLon(), cluster.hashCode(), clusterMarker(cluster));
			}
		}

		private String pointMarker(Object object) {
			return pointMarkers.get(object.hashCode() % pointMarkers.size());
		}

		private String clusterMarker(Object object) {
			return clusterMarkers.get(object.hashCode() % clusterMarkers.size());
		}
	}
}
