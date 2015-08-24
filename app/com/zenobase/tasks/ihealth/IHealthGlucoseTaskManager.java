package com.zenobase.tasks.ihealth;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class IHealthGlucoseTaskManager extends IHealthTaskManagerSupport<IHealthGlucoseTask> {

	@Inject
	public IHealthGlucoseTaskManager(IHealthCredentialsManager credentialsManager) {
		super(IHealthGlucoseTask.TYPE, credentialsManager, IHealthGlucoseTask.class);
		register("glucose", new ResultHandler<IHealthGlucoseTask>() {
			@Override
			public IHealthResultSupport process(IHealthGlucoseTask task, ObjectNode node) {
				return new IHealthGlucoseResult(node, task.getPrincipal(), task.getTag(), task.getTimezone());
			}
		});
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new IHealthGlucoseTask(bucketId, principal, tag, zone, marker);
	}
}
