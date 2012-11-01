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

	public static Status success(String undoId) {
		return result(OK, null, undoId);
	}

	public static Status created(String undoId) {
		return result(CREATED, null, undoId);
	}

	public static Status badRequest(String message) {
		return result(BAD_REQUEST, message);
	}

	public static Status unauthorized(String message) {
		return result(UNAUTHORIZED, message);
	}

	public static Status forbidden(String message) {
		return result(FORBIDDEN, message);
	}

	public static Status notFound(String message) {
		return result(NOT_FOUND, message);
	}

	public static Status conflict(String message) {
		return result(CONFLICT, message);
	}

	public static Status internalServerError(String message) {
		return result(INTERNAL_SERVER_ERROR, message);
	}

	private static Status result(int status, String message) {
		return result(status, message, null);
	}

	private static Status result(int status, String message, String undoId) {
		return status(status, content(message, undoId));
	}

	protected static ObjectNode content(String message, String undoId) {
		ObjectNode node = Nodes.newObject();
		if (message != null) {
			MESSAGE.setValue(node, message);
		}
		if (undoId != null) {
	    	UNDO.setValue(node, undoId);
		}
		return node;
	}
}
