package com.zenobase.tasks.ihealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class IHealthSleepTaskManager extends IHealthTaskManagerSupport<IHealthSleepTask> {

	public IHealthSleepTaskManager(IHealthCredentialsManager credentialsManager, String sv) {
		super(IHealthSleepTask.TYPE, credentialsManager, IHealthSleepTask.class);
		register(
				"sleep",
				sv,
				(task, node) -> new IHealthSleepResult(node, task.getPrincipal(), task.getTag(), task.getTimezone()));
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
