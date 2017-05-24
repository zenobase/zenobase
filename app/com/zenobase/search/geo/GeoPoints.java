package com.zenobase.search.geo;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;

public class GeoPoints {

	private GeoPoints() {

	}

	public static double distance(GeoPoint from, GeoPoint to, DistanceUnit unit) {
		return GeoDistance.ARC.calculate(from.getLat(), from.getLon(),
			to.getLat(), to.getLon(), unit);
	}

	public static GeoPoint copy(GeoPoint point) {
		return new GeoPoint(point.lat(), point.lon());
	}

	public static boolean equals(GeoPoint left, GeoPoint right) {
		return toString(left).equals(toString(right));
	}

	public static String toString(GeoPoint point) {
		return String.format("%.4f,%.4f", point.getLat(), point.getLon());
	}
}
