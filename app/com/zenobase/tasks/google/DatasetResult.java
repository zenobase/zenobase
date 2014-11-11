package com.zenobase.tasks.google;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class DatasetResult extends GoogleFitResultSupport {

	public DatasetResult(JsonNode node, DateTimeZone zone) {
		super(node, zone);
	}

	public List<DataPoint> getDataPoints() {
		List<DataPoint> dataPoints = Lists.newArrayList();
		for (JsonNode pointNode : node.path("point")) {
			addDataPoint(pointNode, dataPoints);
		}
		return dataPoints;
	}

	private void addDataPoint(JsonNode node, List<DataPoint> dataPoints) {
		DateTime begin = dateTimeValue(node.path("startTimeNanos"));
		DateTime end = dateTimeValue(node.path("endTimeNanos"));
		String dataType = node.path("dataTypeName").textValue();
		String origin = node.path("originDataSourceId").textValue();
		BigDecimal[] values = decimalValues(node.path("value"));
		dataPoints.add(new DataPoint(begin, end, dataType, origin, values));
	}

	@Override
	protected DateTime dateTimeValue(JsonNode node) {
		long value = node.asLong();
		Preconditions.checkArgument(value != 0L, "Can't find timestamp: %s", node);
		return new DateTime(value / 1000000, zone);
	}

	private static BigDecimal[] decimalValues(JsonNode node) {
		BigDecimal[] values = new BigDecimal[node.size()];
		for (int i = 0; i < node.size(); ++i) {
			values[i] = Objects.firstNonNull(node.get(i).get("fpVal"), node.get(i).get("intVal")).decimalValue();
		}
		return values;
	}
}
