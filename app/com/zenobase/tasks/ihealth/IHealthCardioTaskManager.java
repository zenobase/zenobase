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

public class IHealthCardioTaskManager extends IHealthTaskManagerSupport<IHealthCardioTask> {

	@Inject
	public IHealthCardioTaskManager(IHealthCredentialsManager credentialsManager, @Named("ihealth.api.sv.bp") String svBp, @Named("ihealth.api.sv.spo2") String svSpO2) {
		super(IHealthCardioTask.TYPE, credentialsManager, IHealthCardioTask.class);
		register("bp", svBp, new ResultHandler<IHealthCardioTask>() {
			@Override
			public IHealthResultSupport process(IHealthCardioTask task, ObjectNode node) {
				return new IHealthBloodPressureResult(node, task.getPrincipal(), task.getTag(), task.getTimezone());
			}
		});
		register("spo2", svSpO2, new ResultHandler<IHealthCardioTask>() {
			@Override
			public IHealthResultSupport process(IHealthCardioTask task, ObjectNode node) {
				return new IHealthBloodOxygenResult(node, task.getPrincipal(), task.getTag(), task.getTimezone());
			}
		});
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new IHealthCardioTask(bucketId, principal, tag, zone, marker);
	}
}
