package com.zenobase.controllers;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.mvc.Controller;
import play.mvc.With;
import com.google.inject.Inject;

import com.zenobase.actions.NoCache;
import com.zenobase.json.Nodes;
import com.zenobase.json.TextField;
import com.zenobase.json.TokenField;

@With(NoCache.class)
public abstract class ControllerSupport extends Controller {

	@Inject
	static SecurityContext auth;

	protected static final TextField MESSAGE = new TextField("message");
	protected static final TokenField UNDO = new TokenField("undo");

	protected static ObjectNode body() {
		return body(ObjectNode.class);
	}

	protected static <T extends JsonNode> T body(Class<T> type) {
		JsonNode node = request().body().asJson();
		return type.isInstance(node) ? type.cast(node) : null;
	}

	public static ObjectNode notification(String message) {
		ObjectNode node = Nodes.newObject();
		node.put("message", message);
		return node;
	}

	protected static ObjectNode receipt(String undoId) {
    	ObjectNode node = Nodes.newObject();
    	UNDO.setValue(node, undoId);
		return node;
	}
}
