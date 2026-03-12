package com.zenobase.models;

import java.math.BigDecimal;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class Location {

	public static final Location ORIGIN = new Location("51.48", "0.00");

	private final BigDecimal latitude, longitude;

	public Location(BigDecimal latitude, BigDecimal longitude) {
		this.latitude = Preconditions.checkNotNull(latitude);
		this.longitude = Preconditions.checkNotNull(longitude);
		Preconditions.checkArgument(isValid(), "Coordinate out of range: %s", this);
	}

	public boolean isValid() {
		double lat = latitude.doubleValue();
		double lon = longitude.doubleValue();
		return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
	}

	public Location(String latitude, String longitude) {
		this(new BigDecimal(latitude), new BigDecimal(longitude));
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Location &&
			equals((Location) that);
	}

	private boolean equals(Location that) {
		return latitude.equals(that.getLatitude()) &&
			longitude.equals(that.getLongitude());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(latitude, longitude);
	}

	@Override
	public String toString() {
		return String.format("%s,%s", latitude, longitude);
	}
}
