package com.zenobase.tasks.withings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.commands.Command;
import com.zenobase.common.Units;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import jakarta.inject.Inject;
import java.util.Objects;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

public class WithingsStepsTaskManager extends WithingsTaskManagerSupport<WithingsStepsTask> {

	@Inject
	public WithingsStepsTaskManager(WithingsCredentialsManager credentialsManager) {
		super(WithingsStepsTask.TYPE, WithingsStepsTask.class, credentialsManager);
	}

	@Override
	public WithingsStepsTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "steps");
		Unit<Length> lengthUnit = MoreObjects.firstNonNull(new UnitField<Length>("unit").getValue(settings), Units.KM);
		String marker = parseMarker(settings.path("marker").textValue());
		return new WithingsStepsTask(bucketId, principal, tag, lengthUnit, Units.KCAL, marker);
	}

	private static @Nullable String parseMarker(@Nullable String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate().toString() : null;
	}

	@Override
	Command safeExecute(WithingsStepsTask task, OAuthCredentials credentials, Token token) {
		OAuthRequest request = createRequest(task);
		Response response = send(request, credentials);
		var result = new WithingsStepsResult(
			parseObject(response),
			task.getPrincipal(),
			Objects.requireNonNull(task.getTag()),
			Objects.requireNonNull(task.getDistanceUnit()),
			task.getHeightUnit(),
			task.getEnergyUnit()
		);
		checkStatus(result, request, credentials);
		return createCommand(task, credentials, token, result);
	}

	private OAuthRequest createRequest(WithingsStepsTask task) {
		var request = new OAuthRequest(Verb.GET, "https://wbsapi.withings.net/v2/measure");
		request.addQuerystringParameter("action", "getactivity");
		request.addQuerystringParameter("startdateymd", LocalDate.parse(task.getMarker()).toString());
		request.addQuerystringParameter("enddateymd", LocalDate.now().toString());
		return request;
	}
}
