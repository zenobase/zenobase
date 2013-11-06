package com.zenobase.tasks.fitbit;

import javax.inject.Named;

import com.google.inject.Inject;

import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.OAuthCredentialsManager;

public class FitbitCredentialsManager extends OAuthCredentialsManager {

	private static final String TYPE = "fitbit";

	@Inject
	public FitbitCredentialsManager(CredentialsRepository repository, @Named("fitbit.api.key") String apiKey, @Named("fitbit.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, repository, new FitbitApi(), apiKey, apiSecret, callbackUrl);
	}
}
