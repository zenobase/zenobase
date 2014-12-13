package com.zenobase.tasks.ihealth;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class IHealthWeightTaskManager extends IHealthTaskManagerSupport<IHealthWeightTask> {

	@Inject
	public IHealthWeightTaskManager(IHealthCredentialsManager credentialsManager) {
		super(IHealthWeightTask.TYPE, credentialsManager, IHealthWeightTask.class);
		register("weight", new ResultHandler<IHealthWeightTask>() {
			@Override
			public IHealthResultSupport process(IHealthWeightTask task, ObjectNode node) {
				return new IHealthWeightResult(node, task.getPrincipal(), task.getTag(), task.getTimezone());
			}
		});
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new IHealthWeightTask(bucketId, principal, tag, zone, marker);
	}
}
