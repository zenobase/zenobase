package com.zenobase.tasks.hexoskin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.joda.time.DateTimeZone;
import org.scribe.model.Response;

import com.zenobase.models.Identity;

public class HexoskinActivitiesTaskManager extends HexoskinTaskManagerSupport<HexoskinActivitiesTask> {

	@Inject
	public HexoskinActivitiesTaskManager(HexoskinCredentialsManager credentialsManager) {
		super(HexoskinActivitiesTask.TYPE, HexoskinActivitiesTask.class, credentialsManager);
	}

	@Override
	public HexoskinTaskSupport newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = settings.path("tag").textValue();
		DateTimeZone zone = DateTimeZone.forID(settings.path("timezone").textValue());
		return new HexoskinActivitiesTask(bucketId, principal, tag, zone, marker);
	}

	@Override
	String getPath(HexoskinTaskSupport task) {
		return (
			"/api/v1/range/?limit=100&order_by=start&rank=0&include_metrics=44,71,2003,2038&start__gt=" +
			task.getStart()
		);
	}

	@Override
	protected HexoskinResultSupport parse(
		Response response,
		HexoskinProfileResult profile,
		HexoskinActivitiesTask task
	) {
		return new HexoskinActivitiesResult(
			parseObject(response),
			task.getPrincipal(),
			task.getTag(),
			task.getZone(),
			profile.isMetric()
		);
	}
}
