package com.zenobase.models;

import java.math.BigDecimal;

public class Location {

	private final BigDecimal latitude, longitude;

	public Location(BigDecimal latitude, BigDecimal longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	@Override
	public String toString() {
		return String.format("%s,%s", latitude, longitude);
	}
}
