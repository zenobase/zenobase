package com.zenobase.search.geo;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.io.GeohashUtils;
import com.spatial4j.core.shape.Rectangle;
import org.elasticsearch.common.geo.GeoPoint;

public class GeoCluster {

	private final long count;
	private final String geohash;
	private final GeoPoint center;

	public GeoCluster(long count, String geohash, GeoPoint center) {
		this.count = count;
		this.geohash = geohash;
		this.center = center;
	}

	public GeoCluster merge(GeoCluster that) {
		long count = this.count + that.count();
		GeoPoint center = mean(this.center, count - that.count(), that.center(), that.count());
		return new GeoCluster(count, geohash, center);
	}

	private static GeoPoint mean(GeoPoint left, long leftWeight, GeoPoint right, long rightWeight) {
		double lat = (left.getLat() * leftWeight + right.getLat() * rightWeight) / (leftWeight + rightWeight);
		double lon = (left.getLon() * leftWeight + right.getLon() * rightWeight) / (leftWeight + rightWeight);
		return new GeoPoint(lat, lon);
	}

	public long count() {
		return count;
	}

	public String geohash() {
		return geohash;
	}

	public GeoPoint center() {
		return center;
	}

	public GeoBoundingBox bounds() {
		Rectangle bounds = GeohashUtils.decodeBoundary(geohash, SpatialContext.GEO);
		return new GeoBoundingBox(new GeoPoint(bounds.getMaxY(), bounds.getMinX()), new GeoPoint(bounds.getMinY(), bounds.getMaxX()));
	}
}
