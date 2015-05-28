package com.zenobase.models;

import com.zenobase.json.BooleanField;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.IntegerField;
import com.zenobase.json.TokenField;

public class UserProfile extends UserInfo {

	private static final DateTimeField CREATED = new DateTimeField("created");
	private static final TokenField EMAIL = new TokenField("email");
	private static final BooleanField VERIFIED = new BooleanField("verified");
	private static final BooleanField SUSPENDED = new BooleanField("suspended");
	private static final BooleanField SUPERUSER = new BooleanField("superuser");
	private static final BooleanField OPTEDOUT = new BooleanField("optedout");
	private static final IntegerField QUOTA = new IntegerField("quota");

	public UserProfile(User user) {
		super(user);
		setValue(CREATED, user.getCreated());
		setValue(EMAIL, user.getEmail());
		if (user.isVerified()) {
			setValue(VERIFIED, true);
		}
		if (user.isSuspended()) {
			setValue(SUSPENDED, true);
		}
		if (user.isSuperuser()) {
			setValue(SUPERUSER, true);
		}
		if (user.isOptedOut()) {
			setValue(OPTEDOUT, true);
		}
		setValue(QUOTA, user.getQuota());
	}
}
