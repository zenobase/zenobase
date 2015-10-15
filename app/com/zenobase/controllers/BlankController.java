package com.zenobase.controllers;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

/*
 * Generates a blank response with minimal headers.
 * Used in the fake iframe that tries to trick Chrome into saving passwords.
 */
public class BlankController extends Controller {

	public static Result get() {
		return status(Http.Status.OK, "");
    }
}
