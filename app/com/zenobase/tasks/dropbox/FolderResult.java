package com.zenobase.tasks.dropbox;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

public class FolderResult {

	private final JsonNode node;

	public FolderResult(JsonNode node) {
		this.node = node;
	}

	public List<String> getFiles() {
		List<String> files = Lists.newArrayList();
		for (JsonNode contentNode : node.path("contents")) {
			String path = contentNode.path("path").textValue();
			int p = path.lastIndexOf('/');
			files.add(path.substring(p + 1));
		}
		return files;
	}
}
