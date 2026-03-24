package com.zenobase.tasks.ihealth;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class IHealthFoodTaskManager extends IHealthTaskManagerSupport<IHealthFoodTask> {

	@Inject
	public IHealthFoodTaskManager(IHealthCredentialsManager credentialsManager, @Named("ihealth.api.sv.food") String sv) {
		super(IHealthFoodTask.TYPE, credentialsManager, IHealthFoodTask.class);
		register("food", sv, (task, node) -> new IHealthFoodResult(node, task.getPrincipal(), task.getTag(), task.getTimezone()));
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new IHealthFoodTask(bucketId, principal, tag, zone, marker);
	}
}
