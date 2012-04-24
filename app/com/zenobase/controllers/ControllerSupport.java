package com.zenobase.controllers;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.Controller;

import com.zenobase.common.Nodes;
import com.zenobase.schema.TokenField;

public abstract class ControllerSupport extends Controller {

	protected static final TokenField UNDO = new TokenField("undo");

	protected static ObjectNode body() {
		return (ObjectNode) request().body().asJson();
	}

	protected static ObjectNode receipt(String undoId) {
    	ObjectNode node = Nodes.newObject();
    	UNDO.setValue(node, undoId);
		return node;
	}
}
