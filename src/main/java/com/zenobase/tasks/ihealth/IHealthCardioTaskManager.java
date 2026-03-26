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

public class IHealthCardioTaskManager extends IHealthTaskManagerSupport<IHealthCardioTask> {

	@Inject
	public IHealthCardioTaskManager(
			IHealthCredentialsManager credentialsManager,
			@Named("ihealth.api.sv.bp") String svBp,
			@Named("ihealth.api.sv.spo2") String svSpO2) {
		super(IHealthCardioTask.TYPE, credentialsManager, IHealthCardioTask.class);
		register(
				"bp",
				svBp,
				(task, node) ->
						new IHealthBloodPressureResult(node, task.getPrincipal(), task.getTag(), task.getTimezone()));
		register(
				"spo2",
				svSpO2,
				(task, node) ->
						new IHealthBloodOxygenResult(node, task.getPrincipal(), task.getTag(), task.getTimezone()));
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(
				MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new IHealthCardioTask(bucketId, principal, tag, zone, marker);
	}
}
