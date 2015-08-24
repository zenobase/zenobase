package com.zenobase.tasks.ihealth;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class IHealthActivitiesTaskManager extends IHealthTaskManagerSupport<IHealthActivitiesTask> {

	@Inject
	public IHealthActivitiesTaskManager(IHealthCredentialsManager credentialsManager) {
		super(IHealthActivitiesTask.TYPE, credentialsManager, IHealthActivitiesTask.class);
		register("sport", new ResultHandler<IHealthActivitiesTask>() {
			@Override
			public IHealthResultSupport process(IHealthActivitiesTask task, ObjectNode node) {
				return new IHealthActivitiesResult(node, task.getPrincipal(), task.getTag(), task.getTimezone());
			}
		});
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new IHealthActivitiesTask(bucketId, principal, tag, zone, marker);
	}
}
