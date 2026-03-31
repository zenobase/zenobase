package com.zenobase.tasks.ihealth;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class IHealthStepsTaskManager extends IHealthTaskManagerSupport<IHealthStepsTask> {

	public IHealthStepsTaskManager(IHealthCredentialsManager credentialsManager, String sv) {
		super(IHealthStepsTask.TYPE, credentialsManager, IHealthStepsTask.class);
		register(
				"activity",
				sv,
				(task, node) -> new IHealthStepsResult(
						node, task.getPrincipal(), Objects.requireNonNull(task.getTag()), task.getTimezone()));
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(
				MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new IHealthSleepTask(bucketId, principal, tag, zone, marker);
	}
}
