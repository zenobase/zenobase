package com.zenobase.controllers;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.Controller;

public abstract class ControllerSupport extends Controller {

	protected static ObjectNode body() {
		return (ObjectNode) request().body().asJson();
	}
}
