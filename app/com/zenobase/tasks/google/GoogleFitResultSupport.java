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

	private static final Resource DEFAULT_SOURCE = new Resource("Google Fit", "https://fit.google.com/");
	private static final ImmutableMap<String, Resource> SOURCES = ImmutableMap.<String, Resource>builder()
		.put("com.azumio.instantheartrate.full", new Resource("Azumio", "http://www.azumio.com/"))
		.put("com.fatsecret.android", new Resource("FatSecret", "https://www.fatsecret.com/"))
		.put("com.fitnesskeeper.runkeeper.pro", new Resource("RunKeeper", "http://runkeeper.com/"))
		.put("com.google.android.maps.mytracks", new Resource("MyTracks", "https://google.com/"))
		.put("com.mapmyrun.android2", new Resource("MapMyRun", "http://www.mapmyrun.com/"))
		.put("com.nike.plusgps", new Resource("Nike+", "http://nikeplus.nike.com/"))
		.put("com.northpark.pushups", new Resource("Push Ups Workout", "https://play.google.com/store/apps/details?id=com.northpark.pushups"))
		.put("com.popularapp.sevenmins", new Resource("7 Minute Workout", "https://play.google.com/store/apps/details?id=com.popularapp.sevenmins"))
		.put("com.runtastic.android", new Resource("Runtastic", "https://www.runtastic.com/"))
		.put("com.runtastic.android.pro2", new Resource("Runtastic", "https://www.runtastic.com/"))
		.put("com.sillens.shapeupclub", new Resource("Lifesum", "https://lifesum.com/"))
		.put("com.strava", new Resource("Strava", "http://www.strava.com/"))
		.put("com.vitrox.facion.gui", new Resource("What's My Heart Rate", "http://facion.net/wmhr"))
		.put("fi.polar.beat", new Resource("Polar Beat", "http://www.polar.com/beat/"))
		.put("si.modula.android.instantheartrate", new Resource("Azumio", "http://www.azumio.com/"))
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
			resource = SOURCES.get(packageName);
			if (resource == null && !packageName.startsWith("com.google")) {
				Logger.warn("Found new package: {}", packageName);
			}
		}
		return Objects.firstNonNull(resource, DEFAULT_SOURCE);
	}
}
