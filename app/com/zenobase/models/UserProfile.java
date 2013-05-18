package com.zenobase.models;

import com.zenobase.json.BooleanField;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IntegerField;
import com.zenobase.json.TokenField;

public class UserProfile extends DomainNode {

	private static final TokenField ID = new TokenField("@id");
	private static final TokenField NAME = new TokenField("name", false);
	private static final DateTimeField CREATED = new DateTimeField("created");
	private static final TokenField EMAIL = new TokenField("email");
	private static final TokenField SECRET = new TokenField("secret");
	private static final BooleanField VERIFIED = new BooleanField("verified");
	private static final BooleanField SUSPENDED = new BooleanField("suspended");
	private static final BooleanField SUPERUSER = new BooleanField("superuser");
	private static final IntegerField QUOTA = new IntegerField("quota");

	public UserProfile(User user) {
		setValue(ID, user.getId());
		setValue(NAME, user.getName());
		setValue(CREATED, user.getCreated());
		setValue(EMAIL, user.getEmail());
		setValue(SECRET, user.getHashedPassword());
		setValue(VERIFIED, user.isVerified());
		setValue(SUSPENDED, user.isSuspended());
		setValue(SUPERUSER, user.isSuperuser());
		setValue(QUOTA, user.getQuota());
	}
}
