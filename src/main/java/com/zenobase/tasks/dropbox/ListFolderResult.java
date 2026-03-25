package com.zenobase.tasks.dropbox;

import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;

public class ListFolderResult {

	private final JsonNode node;

	public ListFolderResult(JsonNode node) {
		this.node = node;
	}

	public boolean hasMore() {
		return node.path("has_more").booleanValue();
	}

	public String getCursor() {
		return node.path("cursor").textValue();
	}

	public List<String> getFiles() {
		List<String> files = new ArrayList<>();
		for (JsonNode entryNode : node.path("entries")) {
			if ("file".equals(entryNode.path(".tag").textValue())) {
				String path = entryNode.path("path_lower").textValue();
				int p = path.lastIndexOf('/');
				files.add(path.substring(p + 1));
			}
		}
		return files;
	}
}
