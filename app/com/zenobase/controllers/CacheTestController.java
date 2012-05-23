package com.zenobase.controllers;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;
import play.mvc.Result;

public class CacheTestController extends ControllerSupport {

	private static final String DATE_FORMAT = "EEEE, dd MMM yyyy hh:mm:ss 'GMT'";
	private static final String DATE_MODIFIED = new DateTime(2012, 1, 1, 8, 0, DateTimeZone.UTC).toString(DATE_FORMAT);
	private static final String HASH = "\"48bfab0b5fd28ba6b11ba46df242925aa73b3747\"";

	public static Result get(String message) {
		String etag = request().getHeader(IF_NONE_MATCH);
		Logger.info(IF_NONE_MATCH + "=" + etag);
		if (HASH.equals(etag)) {
			return status(NOT_MODIFIED);
		}
		String date = request().getHeader(IF_MODIFIED_SINCE);
		Logger.info(IF_MODIFIED_SINCE + "=" + date);
		if (DATE_MODIFIED.equals(date)) {
			return status(NOT_MODIFIED);
		}
		// response().setHeader(ETAG, HASH);
		response().setHeader(DATE, new DateTime(DateTimeZone.UTC).toString(DATE_FORMAT));
		response().setHeader(LAST_MODIFIED, DATE_MODIFIED);
		response().setHeader(EXPIRES, new DateTime(DateTimeZone.UTC).plusMinutes(5).toString(DATE_FORMAT));
		// response().setHeader(CACHE_CONTROL, "max-age=300");
		return ok(message + "\n");
    }
}
