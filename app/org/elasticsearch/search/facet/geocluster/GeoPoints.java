package org.elasticsearch.search.facet.geocluster;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.mapper.geo.GeoPoint;
import org.elasticsearch.index.search.geo.GeoDistance;

public class GeoPoints {

	private GeoPoints() {
		
	}

	public static double distance(GeoPoint from, GeoPoint to, DistanceUnit unit) {
		return GeoDistance.ARC.calculate(from.getLat(), from.getLon(),
			to.getLat(), to.getLon(), unit);
	}

	public static String toString(GeoPoint point) {
		return String.format("%.4f,%.4f", point.getLat(), point.getLon());
	}
}
