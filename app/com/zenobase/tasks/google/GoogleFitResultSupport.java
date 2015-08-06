package com.zenobase.tasks.google;

import com.zenobase.models.Resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;

abstract class GoogleFitResultSupport {

	private static final Resource DEFAULT_SOURCE = new Resource("Google Fit", "https://fit.google.com/");
	private static final ImmutableMap<String, Resource> SOURCES = ImmutableMap.<String, Resource>builder()
		.put("com.azumio.instantheartrate.full", new Resource("Azumio", "http://www.azumio.com/"))
		.put("com.bidusoft.plexfit", new Resource("PlexFit", "http://bidusoft.blogspot.com/"))
		.put("com.dungelin.heartrate", new Resource("Heart Rate Plus", "https://play.google.com/store/apps/details?id=com.dungelin.heartrate"))
		.put("com.endomondo.android", new Resource("Endomondo", "https://www.endomondo.com/"))
		.put("com.fatsecret.android", new Resource("FatSecret", "https://www.fatsecret.com/"))
		.put("com.fitnesskeeper.runkeeper.pro", new Resource("RunKeeper", "http://runkeeper.com/"))
		.put("com.google.android.maps.mytracks", new Resource("MyTracks", "https://google.com/"))
		.put("com.iforpowell.android.ipbike", new Resource("IpBike", "http://www.iforpowell.com/cms/"))
		.put("com.mapmyfitness.android2", new Resource("MapMyFitness", "http://www.mapmyfitness.com/"))
		.put("com.mapmyrun.android2", new Resource("MapMyRun", "http://www.mapmyrun.com/"))
		.put("com.mybasis.android.basis.peak", new Resource("Basis", "http://www.mybasis.com/"))
		.put("com.nike.plusgps", new Resource("Nike+", "http://nikeplus.nike.com/"))
		.put("com.wsl.noom", new Resource("Noom", "http://noom.com/"))
		.put("com.northpark.pushups", new Resource("Push Ups Workout", "https://play.google.com/store/apps/details?id=com.northpark.pushups"))
		.put("com.popularapp.sevenmins", new Resource("7 Minute Workout", "https://play.google.com/store/apps/details?id=com.popularapp.sevenmins"))
		.put("com.runtastic.android", new Resource("Runtastic", "https://www.runtastic.com/"))
		.put("com.runtastic.android.pro2", new Resource("Runtastic", "https://www.runtastic.com/"))
		.put("com.ryansteckler.heartsync", new Resource("HeartSync", "https://play.google.com/store/apps/details?id=com.ryansteckler.heartsync"))
		.put("com.ryansteckler.perfectcinch", new Resource("Cinch Weight Loss and Fitness", "http://www.perfectcinch.com/"))
		.put("com.sillens.shapeupclub", new Resource("Lifesum", "https://lifesum.com/"))
		.put("com.strava", new Resource("Strava", "http://www.strava.com/"))
		.put("com.ua.record", new Resource("Under Armour Record", "https://record.underarmour.com/"))
		.put("com.urbandroid.sleep", new Resource("Sleep as Android", "https://sites.google.com/site/sleepasandroid/"))
		.put("com.vitrox.facion.gui", new Resource("What's My Heart Rate", "http://facion.net/wmhr"))
		.put("com.withings", new Resource("Withings", "http://www.withings.com/us/"))
		.put("com.withings.wiscale2", new Resource("Withings", "http://www.withings.com/us/"))
		.put("com.xiaomi.hm.health", new Resource("Mi Fit", "http://www.mi.com/"))
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
