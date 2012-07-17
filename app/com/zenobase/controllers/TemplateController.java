package com.zenobase.controllers;

import play.cache.Cached;
import play.mvc.Result;

public class TemplateController extends ControllerSupport {

	@Cached(key = "index")
	public static Result index() {
		return ok(views.html.index.render());
	}
}
