package com.zenobase.tasks.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.models.Resource;

abstract class GoogleFitResultSupport {

	private static final Logger logger = LoggerFactory.getLogger(GoogleFitResultSupport.class);

	private static final Resource DEFAULT_SOURCE = new Resource("Google Fit", "https://fit.google.com/");
	private static final ImmutableMap<String, Resource> SOURCES = ImmutableMap.<String, Resource>builder()
			.put("com.alivecor.aliveecg", new Resource("AliceCor", "https://www.alivecor.com/"))
			.put("com.azumio.instantheartrate.full", new Resource("Azumio", "https://www.azumio.com/"))
			.put("com.bidusoft.plexfit", new Resource("PlexFit", "https://bidusoft.blogspot.com/"))
			.put("com.calm.android", new Resource("Calm", "https://www.calm.com/"))
			.put(
					"com.dungelin.heartrate",
					new Resource(
							"Heart Rate Plus", "https://play.google.com/store/apps/details?id=com.dungelin.heartrate"))
			.put("com.emberify.instant", new Resource("Instant", "https://instantapp.today/"))
			.put("com.endomondo.android", new Resource("Endomondo", "https://www.endomondo.com/"))
			.put("com.fatsecret.android", new Resource("FatSecret", "https://www.fatsecret.com/"))
			.put("com.fitnesskeeper.runkeeper.pro", new Resource("RunKeeper", "https://runkeeper.com/"))
			.put("com.fitnow.loseit", new Resource("Lose It!", "https://www.loseit.com/"))
			.put("com.google.android.maps.mytracks", new Resource("MyTracks", "https://google.com/"))
			.put("com.getsomeheadspace.android", new Resource("Headspace", "https://www.headspace.com/"))
			.put("com.iforpowell.android.ipbike", new Resource("IpBike", "http://www.iforpowell.com/cms/"))
			.put("com.mapmyfitness.android2", new Resource("MapMyFitness", "https://www.mapmyfitness.com/"))
			.put("com.mapmyhike.android2", new Resource("MapMyHike", "https://www.mapmyhike.com/"))
			.put("com.mapmyrun.android2", new Resource("MapMyRun", "https://www.mapmyrun.com/"))
			.put("com.mapmywalk.android2", new Resource("MapMyWalk", "https://www.mapmywalk.com/"))
			.put("com.misfitwearables.prometheus", new Resource("Misfit", "https://misfit.com/"))
			.put(
					"com.motorola.omni",
					new Resource("Moto Body", "https://www.motorola.com/us/software-and-apps/moto-body"))
			.put("com.mybasis.android.basis.peak", new Resource("Basis", "https://www.mybasis.com/"))
			.put("com.myfitnesspal.android", new Resource("MyFitnessPal", "https://www.myfitnesspal.com/"))
			.put("com.nike.plusgps", new Resource("Nike+", "http://nikeplus.nike.com/"))
			.put("com.wsl.noom", new Resource("Noom", "https://www.noom.com/"))
			.put(
					"com.northpark.pushups",
					new Resource(
							"Push Ups Workout", "https://play.google.com/store/apps/details?id=com.northpark.pushups"))
			.put(
					"com.popularapp.sevenmins",
					new Resource(
							"7 Minute Workout",
							"https://play.google.com/store/apps/details?id=com.popularapp.sevenmins"))
			.put("com.runtastic.android", new Resource("Runtastic", "https://www.runtastic.com/"))
			.put("com.runtastic.android.pro2", new Resource("Runtastic", "https://www.runtastic.com/"))
			.put(
					"com.ryansteckler.heartsync",
					new Resource(
							"HeartSync", "https://play.google.com/store/apps/details?id=com.ryansteckler.heartsync"))
			.put(
					"com.ryansteckler.perfectcinch",
					new Resource("Cinch Weight Loss and Fitness", "http://www.perfectcinch.com/"))
			.put("com.sillens.shapeupclub", new Resource("Lifesum", "https://lifesum.com/"))
			.put("com.strava", new Resource("Strava", "https://www.strava.com/"))
			.put("com.ua.record", new Resource("Under Armour Record", "https://record.underarmour.com/"))
			.put(
					"com.urbandroid.sleep",
					new Resource("Sleep as Android", "https://sites.google.com/site/sleepasandroid/"))
			.put("com.vitrox.facion.gui", new Resource("What's My Heart Rate", "https://facion.net/wmhr"))
			.put("com.withings", new Resource("Withings", "https://www.withings.com/us/"))
			.put("com.withings.wiscale2", new Resource("Withings", "https://www.withings.com/us/"))
			.put("com.xiaomi.hm.health", new Resource("Mi Fit", "http://www.mi.com/"))
			.put("fi.polar.beat", new Resource("Polar Beat", "https://www.polar.com/beat/"))
			.put("fi.polar.polarflow", new Resource("Polar Flow", "https://flow.polar.com/"))
			.put("si.modula.android.instantheartrate", new Resource("Azumio", "https://www.azumio.com/"))
			.build();
	private static final Cache<String, Boolean> NEW_SOURCES =
			CacheBuilder.newBuilder().maximumSize(100).build();

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
			if (resource == null
					&& !packageName.startsWith("com.google")
					&& NEW_SOURCES.getIfPresent(packageName) == null) {
				logger.warn("Found new package: {}", packageName);
				NEW_SOURCES.put(packageName, Boolean.TRUE);
			}
		}
		return MoreObjects.firstNonNull(resource, DEFAULT_SOURCE);
	}
}
