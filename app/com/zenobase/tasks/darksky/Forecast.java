package com.zenobase.tasks.darksky;

import java.util.Set;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

public class Forecast {

	private final String summary;
	private final DecimalMeasure<Temperature> temperature;
	private final DecimalMeasure<Pressure> pressure;
	private final Integer humidity;
	private final Percentage moon;

	public Forecast(String summary, DecimalMeasure<Temperature> temperature, DecimalMeasure<Pressure> pressure, Integer humidity, Percentage moon) {
		this.summary = summary;
		this.temperature = temperature;
		this.pressure = pressure;
		this.humidity = humidity;
		this.moon = moon;
	}

	public boolean apply(Event event, Set<String> fields) {
		boolean updated = false;
		if (event.contains(Event.LOCATION)) {
			if (summary != null && fields.contains(Event.TAG.getName())) {
				event.addValue(Event.TAG, summary);
				updated = true;
			}
			if (temperature != null && fields.contains(Event.TEMPERATURE.getName())) {
				event.setValue(Event.TEMPERATURE, temperature);
				updated = true;
			}
			if (pressure != null && fields.contains(Event.PRESSURE.getName())) {
				event.setValue(Event.PRESSURE, pressure);
				updated = true;
			}
			if (humidity != null && fields.contains(Event.HUMIDITY.getName())) {
				event.setValue(Event.HUMIDITY, humidity);
				updated = true;
			}
		}
		if (moon != null && fields.contains(Event.MOON.getName())) {
			event.setValue(Event.MOON, moon);
			updated = true;
		}
		if (updated) {
			event.addValue(Event.SOURCE, newResource(event));
		}
		return updated;
	}

	private static Resource newResource(Event event) {
		DateTime timestamp = Ordering.natural().min(event.getValues(Event.TIMESTAMP));
		Location location = Iterables.getFirst(event.getValues(Event.LOCATION), null);
		String url = "https://forecast.io/";
		if (location != null) {
			url += String.format("#/f/%s,%s/%d",
				location.getLatitude(), location.getLongitude(),
				timestamp.getMillis() / 1000L);
		}
		return new Resource("Forecast", url);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("summary", summary)
			.add("temperature", temperature)
			.add("pressure", pressure)
			.add("humidity", humidity)
			.add("moon", moon)
			.toString();
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Forecast
			&& equals((Forecast) that);
	}

	private boolean equals(Forecast that) {
		return Objects.equal(summary, that.summary)
			&& Objects.equal(temperature, that.temperature)
			&& Objects.equal(pressure, that.pressure)
			&& Objects.equal(humidity, that.humidity)
			&& Objects.equal(moon, that.moon);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(summary, temperature, pressure, humidity, moon);
	}
}
