package com.zenobase.tasks.ihealth;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class IHealthWeightTaskManager extends IHealthTaskManagerSupport<IHealthWeightTask> {

	@Inject
	public IHealthWeightTaskManager(
			IHealthCredentialsManager credentialsManager, @Named("ihealth.api.sv.weight") String sv) {
		super(IHealthWeightTask.TYPE, credentialsManager, IHealthWeightTask.class);
		register(
				"weight",
				sv,
				(task, node) -> new IHealthWeightResult(
						node,
						task.getPrincipal(),
						Objects.requireNonNull(task.getTag()),
						task.getTimezone()));
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(
				MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new IHealthWeightTask(bucketId, principal, tag, zone, marker);
	}
}
