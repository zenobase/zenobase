package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;

public class RedirectController extends ControllerSupport {

	@Inject
	public RedirectController(AuthorizationContext security) {
		super(security);
	}

	public Result get(String url) {
    	return found(url);
    }
}
