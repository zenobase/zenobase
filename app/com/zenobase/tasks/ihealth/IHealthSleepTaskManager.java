package com.zenobase.tasks.ihealth;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class IHealthSleepTaskManager extends IHealthTaskManagerSupport<IHealthSleepTask> {

	@Inject
	public IHealthSleepTaskManager(IHealthCredentialsManager credentialsManager, @Named("ihealth.api.sv.sleep") String sv) {
		super(IHealthSleepTask.TYPE, credentialsManager, IHealthSleepTask.class);
		register("sleep", sv, new ResultHandler<IHealthSleepTask>() {
			@Override
			public IHealthResultSupport process(IHealthSleepTask task, ObjectNode node) {
				return new IHealthSleepResult(node, task.getPrincipal(), task.getTag(), task.getTimezone());
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
