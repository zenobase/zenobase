package com.zenobase.tasks;

import javax.inject.Inject;
import javax.inject.Named;

import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.zenobase.commands.Command;

public class TwitterTaskManager extends OAuthTaskManager {

	@Inject
	public TwitterTaskManager(@Named("twitter.api.key") String apiKey, @Named("twitter.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TwitterApi.class, apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return TwitterTask.TYPE;
	}

	@Override
	public Command execute(Task task) {
		return execute(task.as(TwitterTask.class));
	}

	private Command execute(TwitterTask task) {
		OAuthService service = getService(task);
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.twitter.com/1.1/statuses/user_timeline.json");
		service.signRequest(task.getToken(), request);
		Response response = request.send();
		TwitterTimelineNode timeline = new TwitterTimelineNode(parseArray(response));
		System.out.println("tweets: " + timeline.size());
		return null; // TODO
	}
}
