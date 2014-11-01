package com.zenobase.tasks.google;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import com.zenobase.models.Resource;

public class DataSourcesResult extends GoogleFitResultSupport {

	public DataSourcesResult(JsonNode node) {
		super(node, null);
	}

	public List<DataStream> get() {
		List<DataStream> streams = Lists.newArrayList();
		for (JsonNode dataSourceNode : node.path("dataSource")) {
			addDataStream(dataSourceNode, streams);
		}
		return streams;
	}

	private void addDataStream(JsonNode node, List<DataStream> streams) {
		String id = node.path("dataStreamId").textValue();
		String type = node.path("dataType").path("name").textValue();
		Resource source = resourceValue(node.path("application"));
		streams.add(new DataStream(id, type, source));
	}
}
