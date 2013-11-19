package com.zenobase.tasks.fitbit;

import javax.inject.Inject;
import javax.inject.Named;

import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.OAuthCredentialsManager;

public class FitbitCredentialsManager extends OAuthCredentialsManager {

	private static final String TYPE = "fitbit";

	@Inject
	public FitbitCredentialsManager(CredentialsRepository repository, @Named("fitbit.api.key") String apiKey, @Named("fitbit.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, repository, new FitbitApi(), apiKey, apiSecret, callbackUrl);
	}
}
