package com.zenobase.common;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.ImmutableList;

public class DefaultDashboard {
	
	public List<ObjectNode> widgets(){
		return ImmutableList.of(timeline(), list(), map());
	}

	private ObjectNode list(){
		ObjectNode widget = Nodes.newObject();
		widget.put("id", "default-list");
		widget.put("label", "Latest");
		widget.put("type", "list");
		widget.put("placement", "left");
		widget.put("singleton", true);
		widget.put("limit", 5);
		widget.put("order", "timestamp");
		widget.put("reverse", false);
		return widget;
	}

	private ObjectNode timeline(){
		ObjectNode widget = Nodes.newObject();
		widget.put("id", "default-timeline");
		widget.put("label", "Timeline");
		widget.put("type", "timeline");
		widget.put("placement", "top");
		return widget;
	}

	private ObjectNode map(){
		ObjectNode widget = Nodes.newObject();
		widget.put("id", "default-map");
		widget.put("label", "Map");
		widget.put("type", "map");
		widget.put("placement", "right");
		return widget;
	}
}
