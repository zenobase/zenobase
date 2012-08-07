package com.zenobase.controllers;

import java.util.regex.Pattern;

import play.api.mvc.Action;
import play.api.mvc.AnyContent;

/**
 * Custom asset controller that delegates to the default asset controller, after
 * stripping cache buster strings.
 */
public class Assets extends ControllerSupport {

	private static final Pattern CACHE_BUSTER = Pattern.compile("-[0-9a-f]{8}\\b");

	public static Action<AnyContent> at(String path, String file) {
		return controllers.Assets.at(path, stripCacheBuster(file));
	}

	static String stripCacheBuster(String file) {
		return CACHE_BUSTER.matcher(file).replaceAll("");
	}
}
