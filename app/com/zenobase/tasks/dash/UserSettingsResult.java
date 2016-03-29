package com.zenobase.tasks.dash;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.UUID;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Volume;

import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.common.LengthPerVolume;
import com.zenobase.common.Units;

class UserSettingsResult {

	static final BigDecimal FACTOR_UK_TO_US_GAL = new BigDecimal("1.20095");
	static final BigDecimal FACTOR_UK_TO_US_MPG = BigDecimal.ONE.divide(FACTOR_UK_TO_US_GAL, MathContext.DECIMAL32);

	private final JsonNode node;

	public UserSettingsResult(JsonNode node) {
		this.node = node;
	}

	public UserSettings get() {
		UserSettings settings = new UserSettings(getDistanceConverter(), getVolumeConverter(), getFuelEfficiencyConverter());
		for (JsonNode vehicleNode : node.path("vehicles")) {
			settings.addVehicle(UUID.fromString(vehicleNode.path("id").textValue()), vehicleNode.path("name").textValue());
		}
		return settings;
	}

	private DecimalConverter<Length> getDistanceConverter() {
		switch (node.path("preferredUnits").path("distance").textValue()) {
			case "Miles":
				return new DecimalConverter<>(Units.MI);
			case "Kilometers":
				return new DecimalConverter<>(Units.KM);
		}
		throw new IllegalArgumentException("unsupported distance unit in " + node);
	}

	private DecimalConverter<Volume> getVolumeConverter() {
		switch (node.path("preferredUnits").path("volume").textValue()) {
			case "USGallon":
				return new DecimalConverter<>(Units.GAL);
			case "ImperialGallon":
				return new ScaledDecimalConverter<>(Units.GAL, FACTOR_UK_TO_US_GAL);
			case "Liter":
				return new DecimalConverter<>(Units.L);
		}
		throw new IllegalArgumentException("unsupported volume unit in " + node);
	}

	private DecimalConverter<LengthPerVolume> getFuelEfficiencyConverter() {
		switch (node.path("preferredUnits").path("fuelEfficiency").textValue()) {
			case "MilesPerUSGallon":
				return new DecimalConverter<>(Units.MPG);
			case "MilesPerImperialGallon":
				return new ScaledDecimalConverter<>(Units.MPG, FACTOR_UK_TO_US_MPG);
			case "KilometersPerLiter":
				return new DecimalConverter<>(Units.KPL);
			case "LiterPer100Kilometer":
				return new DecimalConverter<LengthPerVolume>(Units.KPL) {
					@Override
					public DecimalMeasure<LengthPerVolume> apply(BigDecimal value) {
						return super.apply(BigDecimal.ONE.divide(value, MathContext.DECIMAL32).scaleByPowerOfTen(2));
					}
				};
		}
		throw new IllegalArgumentException("unsupported fuel efficiency unit in " + node);
	}
}
