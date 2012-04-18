package com.zenobase.schema;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Volume;
import javax.measure.unit.Dimension;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import junit.framework.Assert;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.zenobase.common.Measures;

public class MeasuresTest {

	@Test
	public void test() {
		System.out.println("length: " + getUnits(Dimension.LENGTH, Length.class)); // km=SI.METER.times(1000), cm=SI.METER.divide(100), mm=SI.METER.divide(1000), um=SI.METER.divide(1000000)
		System.out.println("mass: " + getUnits(Dimension.MASS, Mass.class));
		System.out.println("temp: " + getUnits(Dimension.TEMPERATURE, Temperature.class));
		System.out.println("none: " + getUnits(Dimension.NONE, Dimensionless.class));
		System.out.println("volume: " + getUnits(Dimension.LENGTH.pow(3), Volume.class));
		System.out.println("time: " + getUnits(Dimension.TIME, Duration.class));
	}

	private static <Q extends Quantity> Iterable<Unit<Q>> getUnits(Dimension dimension, Class<Q> type) {
		List<Unit<Q>> units = Lists.newArrayList();
		for (Unit<?> unit : SI.getInstance().getUnits()) {
			if (unit.getDimension().equals(dimension)) {
				units.add(unit.asType(type));
			}
		}
		for (Unit<?> unit : NonSI.getInstance().getUnits()) {
			if (unit.getDimension().equals(dimension)) {
				units.add(unit.asType(type));
			}
		}
		return units;
	}

	@Test
	public void testToStandard() {
		assertMeasureEqual("16093.44 m", "10 mi");
		assertMeasureEqual("10000 m", "10 km");
		assertMeasureEqual("68.0388555 kg", "150 lb");
		assertMeasureEqual("294.2611111111112 K", "70 °F");
		assertMeasureEqual("293.15 K", "20 °C");
		assertMeasureEqual("0.001 m³", "1 L");
		assertMeasureEqual("1 m³", "1 m³");
	}

	private void assertMeasureEqual(String expected, String value) {
		Assert.assertEquals(DecimalMeasure.valueOf(expected), Measures.toStandard(DecimalMeasure.valueOf(value)));
	}
}
