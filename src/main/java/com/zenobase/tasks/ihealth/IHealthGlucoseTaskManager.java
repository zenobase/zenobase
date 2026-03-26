package com.zenobase.tasks.ihealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class IHealthGlucoseTaskManager extends IHealthTaskManagerSupport<IHealthGlucoseTask> {

	@Inject
	public IHealthGlucoseTaskManager(
			IHealthCredentialsManager credentialsManager, @Named("ihealth.api.sv.glucose") String sv) {
		super(IHealthGlucoseTask.TYPE, credentialsManager, IHealthGlucoseTask.class);
		register(
				"glucose",
				sv,
				(task, node) -> new IHealthGlucoseResult(node, task.getPrincipal(), task.getTag(), task.getTimezone()));
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(
				MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new IHealthGlucoseTask(bucketId, principal, tag, zone, marker);
	}
}
