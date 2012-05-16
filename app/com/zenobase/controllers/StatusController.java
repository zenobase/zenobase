package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;

import com.zenobase.services.CommandRepository;

public class StatusController extends ControllerSupport {

	@Inject
	static CommandRepository history;

	public static Result get() {
    	return ok(Long.toString(history.size()));
    }
}
