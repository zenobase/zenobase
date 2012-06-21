package com.zenobase.controllers;

import play.api.mvc.Action;
import play.api.mvc.AnyContent;

/**
 * Custom asset controller. Delegates to the default asset controller, but lets
 * us do action composition, e.g. {@link Canonical}.
 */
public class Assets extends ControllerSupport {

	public static Action<AnyContent> at(String path, String file) {
		return controllers.Assets.at(path, file);
	}
}
