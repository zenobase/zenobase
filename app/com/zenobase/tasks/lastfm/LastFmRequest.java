package com.zenobase.tasks.lastfm;

import java.util.Map;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class LastFmRequest extends OAuthRequest {

	private final Map<String, String> params = Maps.newHashMap();

	public LastFmRequest() {
		super(Verb.GET, "http://ws.audioscrobbler.com/2.0/");
	}

	@Override
	public void addQuerystringParameter(String key, String value) {
		super.addQuerystringParameter(key, value);
		params.put(key, value);
	}

	public ImmutableMap<String, String> getQuerystringParameters() {
		return ImmutableMap.copyOf(params);
	}
}
