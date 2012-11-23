package com.zenobase.tasks;

import java.util.List;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;

public class WithingsTaskManager extends OAuthTaskManager<WithingsTask> {

	public WithingsTaskManager(String apiKey, String apiSecret, String callbackUrl) {
		super(WithingsApi.class, apiKey, apiSecret, callbackUrl);
	}

	@Override
	public Command execute(WithingsTask task) {
		OAuthRequest request = createRequest(task);
		getService(task).signRequest(task.getToken(), request);
		Response response = request.send();
		for (Event event : process(task, response)) {
			System.out.println("event: " + event.toJson());
		}
		return null; // TODO
	}

	private static OAuthRequest createRequest(WithingsTask task) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://wbsapi.withings.net/measure");
		request.addQuerystringParameter("userid", Integer.toString(task.getUserId()));
		request.addQuerystringParameter("action", "getmeas");
		request.addQuerystringParameter("devtype", "1"); // weight scale data
		if (task.getMarker() != null) {
			request.addQuerystringParameter("lastupdate", task.getMarker());
		}
		return request;
	}

	public static List<Event> process(WithingsTask task, Response response) {
		WithingsResultNode result = new WithingsResultNode(parseObject(response));
		task.setMarker(result.getMarker());
		System.out.println("marker: " + task.getMarker());
		return result.getEvents();
	}

	@Override
	protected void configure(ServiceBuilder builder) {
		builder.signatureType(SignatureType.QueryString);
	}
}