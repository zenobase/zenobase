package com.zenobase.tasks.hexoskin;

import javax.inject.Inject;
import javax.inject.Named;

import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class HexoskinCredentialsManager extends OAuthCredentialsManager {

	private static final String TYPE = "hexoskin";

	@Inject
	public HexoskinCredentialsManager(CredentialsRepository repository, @Named("hexoskin.api.key") String apiKey, @Named("hexoskin.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, repository, new HexoskinApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}
}
