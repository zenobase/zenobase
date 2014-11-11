package com.zenobase.tasks.google;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import com.zenobase.models.Resource;

abstract class GoogleFitResultSupport {

	private static final Resource DEFAULT_RESOURCE = new Resource("Google Fit", "https://fit.google.com/");
	private static final ImmutableMap<String, Resource> RESOURCES = ImmutableMap.<String, Resource>builder()
		.put("com.fitnesskeeper.runkeeper.pro", new Resource("RunKeeper", "http://runkeeper.com/"))
		.put("com.mapmyrun.android2", new Resource("MapMyRun", "http://www.mapmyrun.com/"))
		.put("com.runtastic.android", new Resource("Runtastic", "https://www.runtastic.com/"))
		.put("com.runtastic.android.pro2", new Resource("Runtastic", "https://www.runtastic.com/"))
		.put("com.strava", new Resource("Strava", "http://www.strava.com/"))
		.put("fi.polar.beat", new Resource("Polar Beat", "http://www.polar.com/beat/"))
		.build();

	protected final JsonNode node;
	protected final DateTimeZone zone;

	public GoogleFitResultSupport(JsonNode node, DateTimeZone zone) {
		this.node = node;
		this.zone = zone;
	}

	protected String activityTypeValue(JsonNode node) {
		return node.isInt() ? ActivityTypes.forID(node.intValue()) : null;
	}

	protected DateTime dateTimeValue(JsonNode node) {
		long value = node.asLong();
		Preconditions.checkArgument(value != 0L, "Can't find timestamp: %s", node);
		return new DateTime(value, zone);
	}

	protected Resource resourceValue(JsonNode node) {
		Resource resource = null;
		String packageName = node.path("packageName").textValue();
		String title = node.path("name").textValue();
		String detailsUrl = node.path("detailsUrl").textValue();
		if (title != null && detailsUrl != null) {
			resource = new Resource(title, detailsUrl);
		} else if (packageName != null) {
			resource = RESOURCES.get(packageName);
			if (resource == null && !packageName.startsWith("com.google")) {
				Logger.warn("Found new package: {}", packageName);
			}
		}
		return Objects.firstNonNull(resource, DEFAULT_RESOURCE);
	}
}
