package com.zenobase.controllers;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.Controller;
import com.google.inject.Inject;

import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;

public abstract class ControllerSupport extends Controller {

	@Inject
	static SecurityContext auth;

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
