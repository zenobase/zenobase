package org.elasticsearch.search.facet.geocluster;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.mapper.geo.GeoPoint;

import com.google.common.base.Preconditions;

public class GeoBoundingBox {
	
	private final GeoPoint topLeft, bottomRight;

	public GeoBoundingBox(GeoPoint topLeft, GeoPoint bottomRight) {
		Preconditions.checkArgument(topLeft.getLat() >= bottomRight.getLat());
		Preconditions.checkArgument(topLeft.getLon() <= bottomRight.getLon());
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}

	public GeoPoint topLeft() {
		return topLeft;
	}

	public GeoPoint bottomRight() {
		return bottomRight;
	}

	public boolean contains(GeoPoint point) {
		return point.getLat() <= topLeft.getLat() && point.getLat() >= bottomRight.getLat() &&
			point.getLon() >= topLeft.getLon() && point.getLon() <= bottomRight.getLon();
	}

	public GeoBoundingBox extend(GeoPoint point) {
		return extend(point, point);
	}

	public GeoBoundingBox extend(GeoBoundingBox bounds) {
		return extend(bounds.topLeft(), bounds.bottomRight());
	}

	private GeoBoundingBox extend(GeoPoint topLeft, GeoPoint bottomRight) {
		return contains(topLeft) && contains(bottomRight) ? this : new GeoBoundingBox(
			new GeoPoint(Math.max(topLeft().getLat(), topLeft.getLat()), Math.min(topLeft().getLon(), topLeft.getLon())),
			new GeoPoint(Math.min(bottomRight().getLat(), bottomRight.getLat()), Math.max(bottomRight().getLon(), bottomRight.getLon())));
	}

	public boolean intersects(GeoBoundingBox bounds) {
		return contains(bounds.topLeft()) || contains(bounds.bottomRight());
	}

	public GeoBoundingBox intersection(GeoBoundingBox bounds) {
		return intersects(bounds) ? new GeoBoundingBox(
			contains(bounds.topLeft()) ? bounds.topLeft() : topLeft, 
			contains(bounds.bottomRight()) ? bounds.bottomRight() : bottomRight) : null;
	}

	public double size(DistanceUnit unit) {
		return GeoPoints.distance(topLeft, bottomRight, unit);
	}
}
