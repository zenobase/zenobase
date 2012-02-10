package org.elasticsearch.search.facet.geocluster;

import java.util.List;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.mapper.geo.GeoPoint;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class GeoClusterBuilder {

	private final double maxClusterDiagonalLength;
	private final DistanceUnit unit;
	private final List<GeoCluster> clusters = Lists.newArrayList();
	private final List<Listener> listeners = Lists.newArrayList();
	
	public GeoClusterBuilder(double maxClusterDiagonalLength, DistanceUnit unit) {
		this.maxClusterDiagonalLength = maxClusterDiagonalLength;
		this.unit = unit;
	}

	public GeoClusterBuilder add(GeoPoint point) {
		GeoCluster cluster = null;
		double distance = Double.MAX_VALUE;
		for (GeoCluster c : clusters) {
			double d = GeoPoints.distance(c.center(), point, unit);
			if (d < distance && d <= maxClusterDiagonalLength && c.bounds().extend(point).size(unit) <= maxClusterDiagonalLength) {
				d = distance;
				cluster = c;
			}
		}
		if (cluster == null) {
			cluster = new GeoCluster();
			clusters.add(cluster);
		}
		cluster.add(point);
		notifyListeners(point, cluster);
		return this;
	}

	public GeoClusterBuilder addAll(Iterable<GeoPoint> points) {
		for (GeoPoint point : points) {
			add(point);
		}
		return this;
	}

	public ImmutableList<GeoCluster> build() {
		return ImmutableList.copyOf(clusters);
	}

	public GeoClusterBuilder addListener(Listener listener) {
		listeners.add(listener);
		return this;
	}

	private void notifyListeners(GeoPoint point, GeoCluster cluster) {
		for (Listener listener : listeners) {
			listener.added(point, cluster);
		}
	}

	public interface Listener {

		void added(GeoPoint point, GeoCluster cluster);
	}
}
