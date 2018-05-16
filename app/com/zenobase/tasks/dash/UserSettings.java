package com.zenobase.tasks.dash;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Volume;

import com.google.common.collect.Maps;

import com.zenobase.common.LengthPerVolume;

class UserSettings {

	private final Map<UUID, String> vehicles = Maps.newHashMap();
	private final DecimalConverter<Length> distance;
	private final DecimalConverter<Volume> volume;
	private final DecimalConverter<LengthPerVolume> fuelEfficiency;

	public UserSettings(DecimalConverter<Length> distance, DecimalConverter<Volume> volume, DecimalConverter<LengthPerVolume> fuelEfficiency) {
		this.distance = distance;
		this.volume = volume;
		this.fuelEfficiency= fuelEfficiency;
			}

	public void addVehicle(UUID id, String name) {
		vehicles.put(id, name);
	}

	public String getVehicle(UUID id) {
		return vehicles.get(id);
	}

	public DecimalMeasure<Length> newDistance(BigDecimal value) {
		return distance.apply(value);
	}

	public DecimalMeasure<Volume> newVolume(BigDecimal value) {
		return volume.apply(value);
	}

	public DecimalMeasure<LengthPerVolume> newFuelEfficiency(BigDecimal value) {
		return fuelEfficiency.apply(value);
	}
}
