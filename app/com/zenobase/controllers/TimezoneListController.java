package com.zenobase.controllers;

import java.util.List;

import org.joda.time.DateTimeZone;
import play.mvc.Controller;
import play.mvc.Result;
import com.google.common.collect.Lists;

import com.zenobase.json.Nodes;

public class TimezoneListController extends Controller {

	public Result get() {
		List<String> timezones = Lists.newArrayList();
		for (String id : DateTimeZone.getAvailableIDs()) {
			timezones.add(id);
		}
		return ok(Nodes.newArray(timezones));
    }
}
