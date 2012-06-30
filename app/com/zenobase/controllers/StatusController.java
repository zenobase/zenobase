package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.services.CommandRepository;

@With(Timed.class)
public class StatusController extends ControllerSupport {

	@Inject
	static CommandRepository history;

	public static Result get() {
    	if (!Http.Context.current().request().queryString().isEmpty()) {
    		throw new RuntimeException("invalid parameters");
    	}
		return ok(Long.toString(history.size()));
    }
}
