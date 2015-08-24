package com.zenobase.tasks.ihealth;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class IHealthStepsTaskManager extends IHealthTaskManagerSupport<IHealthStepsTask> {

	@Inject
	public IHealthStepsTaskManager(IHealthCredentialsManager credentialsManager) {
		super(IHealthStepsTask.TYPE, credentialsManager, IHealthStepsTask.class);
		register("activity", new ResultHandler<IHealthStepsTask>() {
			@Override
			public IHealthResultSupport process(IHealthStepsTask task, ObjectNode node) {
				return new IHealthStepsResult(node, task.getPrincipal(), task.getTag(), task.getTimezone());
			}
		});
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new IHealthSleepTask(bucketId, principal, tag, zone, marker);
	}
}
