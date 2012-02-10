package org.elasticsearch.search.facet.geocluster;

import java.util.List;

import org.elasticsearch.common.unit.DistanceUnit;

import com.google.common.collect.Lists;

public class GeoClusterReducer {

	private final double minDistance;
	private final DistanceUnit unit;
	private final List<Listener> listeners = Lists.newArrayList();

	public GeoClusterReducer(double minDistance, DistanceUnit unit) {
		this.minDistance = minDistance;
		this.unit = unit;
	}

	public List<GeoCluster> reduce(Iterable<GeoCluster> clusters) {
		List<GeoCluster> reduced = Lists.newLinkedList(clusters);
		REDUCE: while (true) {
			for (int i = 0; i < reduced.size(); ++i) {
				for (int j = i + 1; j < reduced.size(); ++j) {
					GeoCluster a = reduced.get(i);
					GeoCluster b = reduced.get(j);
					if (shouldMerge(a, b)) {
						a.add(b);
						reduced.remove(b);
						notifyListeners(b, a);
						continue REDUCE;
					}
				}
			}
			break;
		}
		return reduced;
	}

	private boolean shouldMerge(GeoCluster a, GeoCluster b) {
		// GeoBoundingBox overlap = c1.bounds().intersect(c2.bounds());
		// return overlap != null && overlap.size() > Math.min(c1.bounds().size(), c2.bounds().size()) * 0.5;
		// return a.bounds().intersects(b.bounds()) && a.bounds().extend(b.bounds()).size(unit) <= maxClusterDiagonalLength;
		return GeoPoints.distance(a.center(), b.center(), unit) <= minDistance;
	}

	public GeoClusterReducer addListener(Listener listener) {
		listeners.add(listener);
		return this;
	}

	private void notifyListeners(GeoCluster source, GeoCluster target) {
		for (Listener listener : listeners) {
			listener.merged(source, target);
		}
	}

	public interface Listener {

		void merged(GeoCluster source, GeoCluster target);
	}
}
