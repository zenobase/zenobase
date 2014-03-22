package com.zenobase.tasks.forecast;

import java.util.Set;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.joda.time.DateTime;
import com.google.common.base.Objects;

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

	public void apply(Event event, Set<String> fields) {
		if (fields.contains(Event.TAG.getName())) {
			event.addValue(Event.TAG, summary);
		}
		if (fields.contains(Event.TEMPERATURE.getName())) {
			event.setValue(Event.TEMPERATURE, temperature);
		}
		if (fields.contains(Event.PRESSURE.getName())) {
			event.setValue(Event.PRESSURE, pressure);
		}
		if (fields.contains(Event.HUMIDITY.getName())) {
			event.setValue(Event.HUMIDITY, humidity);
		}
		if (fields.contains(Event.MOON.getName())) {
			event.setValue(Event.MOON, moon);
		}
		event.addValue(Event.SOURCE, newResource(event));
	}

	private static Resource newResource(Event event) {
		DateTime timestamp = event.getValue(Event.TIMESTAMP);
		Location location = event.getValue(Event.LOCATION);
		String url = String.format("http://forecast.io/#/f/%s,%s/%d",
			location.getLatitude(), location.getLongitude(),
			timestamp.getMillis() / 1000L);
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
