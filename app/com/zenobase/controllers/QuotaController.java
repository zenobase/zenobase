package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;

import com.zenobase.oauth.Authorization;
import com.zenobase.services.QuotaManager;

public class QuotaController extends ControllerSupport {

	private final QuotaManager quotas;

	@Inject
	public QuotaController(AuthorizationContext security, QuotaManager quotas) {
		super(security);
		this.quotas = quotas;
	}

	public Result get() {
		Authorization auth = getCurrentAuthorization();
		if (auth != null) {
			return ok(quotas.getQuota(auth.getPrincipal()).toJson());
		}
    	return noContent();
    }
}
