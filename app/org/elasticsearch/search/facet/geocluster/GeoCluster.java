package org.elasticsearch.search.facet.geocluster;

import org.elasticsearch.index.mapper.geo.GeoPoint;

public class GeoCluster {

	private int size;
	private GeoPoint center;
	private GeoBoundingBox bounds;

	public void add(GeoPoint point) {
		++size;
		if (center == null) {
			center = point;
			bounds = new GeoBoundingBox(point, point);
		}
		else {
			center = mean(center, size - 1, point, 1);
			bounds = bounds.extend(point);
		}
	}

	public void add(GeoCluster that) {
		size += that.size();
		if (center == null) {
			center = that.center();
			bounds = that.bounds();
		}
		else {
			center = mean(center, size - that.size(), that.center(), that.size());
			bounds = bounds.extend(that.bounds());
		}
	}

	private static GeoPoint mean(GeoPoint left, int leftWeight, GeoPoint right, int rightWeight) {
		double lat = (left.getLat() * leftWeight + right.getLat() * rightWeight) / (leftWeight + rightWeight);
		double lon = (left.getLon() * leftWeight + right.getLon() * rightWeight) / (leftWeight + rightWeight);
		return new GeoPoint(lat, lon);
	}

	public int size() {
		return size;
	}

	public GeoPoint center() {
		return center;
	}

	public GeoBoundingBox bounds() {
		return bounds;
	}
}
