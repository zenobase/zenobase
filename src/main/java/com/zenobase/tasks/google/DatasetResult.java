package com.zenobase.tasks.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class DatasetResult extends GoogleFitResultSupport {

	public DatasetResult(JsonNode node, DateTimeZone zone) {
		super(node, zone);
	}

	public String getNextPageToken() {
		return node.path("nextPageToken").textValue();
	}

	public List<DataPoint> getDataPoints() {
		List<DataPoint> dataPoints = new ArrayList<>();
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
		Object[] values = objectValues(node.path("value"));
		dataPoints.add(new DataPoint(begin, end, dataType, origin, values));
	}

	@Override
	protected DateTime dateTimeValue(JsonNode node) {
		long value = node.asLong();
		Preconditions.checkArgument(value != 0L, "Can't find timestamp: %s", node);
		return new DateTime(value / 1000000, zone);
	}

	private static Object[] objectValues(JsonNode node) {
		Object[] values = new Object[node.size()];
		for (int i = 0; i < node.size(); ++i) {
			values[i] = objectValue(node.get(i));
		}
		return values;
	}

	private static @Nullable Object objectValue(JsonNode node) {
		if (node.has("fpVal")) {
			return node.get("fpVal").decimalValue();
		}
		if (node.has("intVal")) {
			return node.get("intVal").decimalValue();
		}
		if (node.has("stringVal")) {
			return node.get("stringVal").textValue();
		}
		if (node.has("mapVal")) {
			Map<String, Object> value = Maps.newLinkedHashMap();
			for (JsonNode entryNode : node.get("mapVal")) {
				value.put(entryNode.get("key").textValue(), objectValue(entryNode.get("value")));
			}
			return value;
		}
		return null;
	}
}
