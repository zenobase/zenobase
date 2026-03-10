package com.zenobase.search.geo;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.locationtech.spatial4j.shape.Point;

public class GeoClusterBuilder {

	private final Map<String, GeoCluster> clusters = Maps.newLinkedHashMap();

	public void add(long count, String geohash, Point point) {
		add(new GeoCluster(count, reducePrecision(geohash), point));
	}

	public void add(GeoCluster cluster) {
		clusters.merge(cluster.geohash(), cluster, GeoCluster::merge);
	}

	private static String reducePrecision(String geohash) {
		return !geohash.isEmpty() ? geohash.substring(0, geohash.length() - 1) : geohash;
	}

	public ImmutableList<GeoCluster> build() {
		return ImmutableList.copyOf(clusters.values());
	}
}
