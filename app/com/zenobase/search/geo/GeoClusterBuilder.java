package com.zenobase.search.geo;

import java.util.Map;
import java.util.function.BiFunction;

import com.google.common.collect.Maps;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.geo.GeoPoint;

public class GeoClusterBuilder {

	private final Map<String, GeoCluster> clusters = Maps.newLinkedHashMap();

	public void add(long count, String geohash, GeoPoint point) {
		add(new GeoCluster(count, reducePrecision(geohash), new GeoPoint(point.lat(), point.lon())));
	}

	public void add(GeoCluster cluster) {
		clusters.merge(cluster.geohash(), cluster, new BiFunction<GeoCluster, GeoCluster, GeoCluster>() {
			@Override
			public GeoCluster apply(GeoCluster a, GeoCluster b) {
				return a.merge(b);
			}
		});
	}

	private static String reducePrecision(String geohash) {
		return !geohash.isEmpty() ? geohash.substring(0, geohash.length() - 1) : geohash;
	}

	public ImmutableList<GeoCluster> build() {
		return ImmutableList.copyOf(clusters.values());
	}
}
