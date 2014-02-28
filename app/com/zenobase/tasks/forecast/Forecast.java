package com.zenobase.tasks.forecast;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.joda.time.DateTime;
import com.google.common.base.Objects;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;

public class Forecast {

	private final String summary;
	private final DecimalMeasure<Temperature> temperature;
	private final DecimalMeasure<Pressure> pressure;
	private final Integer humidity;

	public Forecast(String summary, DecimalMeasure<Temperature> temperature, DecimalMeasure<Pressure> pressure, Integer humidity) {
		this.summary = summary;
		this.temperature = temperature;
		this.pressure = pressure;
		this.humidity = humidity;
	}

	public void apply(Event event) {
		event.addValue(Event.TAG, summary);
		event.setValue(Event.TEMPERATURE, temperature);
		event.setValue(Event.PRESSURE, pressure);
		event.setValue(Event.HUMIDITY, humidity);
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
			&& Objects.equal(humidity, that.humidity);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(summary, temperature, pressure, humidity);
	}
}
