package com.zenobase.models;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.util.Objects;

public record Location(BigDecimal latitude, BigDecimal longitude) {
	public Location {
		Objects.requireNonNull(latitude);
		Objects.requireNonNull(longitude);
		Preconditions.checkArgument(
			isValid(latitude, longitude),
			"Coordinate out of range: %s,%s",
			latitude,
			longitude
		);
	}

	public Location(String latitude, String longitude) {
		this(new BigDecimal(latitude), new BigDecimal(longitude));
	}

	@Override
	public String toString() {
		return String.format("%s,%s", latitude, longitude);
	}

	public static boolean isValid(BigDecimal latitude, BigDecimal longitude) {
		double lat = latitude.doubleValue();
		double lon = longitude.doubleValue();
		return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
	}
}
