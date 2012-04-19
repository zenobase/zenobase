package com.zenobase.models;

import com.zenobase.schema.BooleanField;
import com.zenobase.schema.TokenField;

public class UserInfo extends DomainNode {

	private static final TokenField ID = new TokenField("@id");
	private static final TokenField NAME = new TokenField("name", false);
	private static final BooleanField SUSPENDED = new BooleanField("suspended");

	public UserInfo(User user) {
		setValue(ID, user.getId());
		setValue(NAME, user.getName());
		setValue(SUSPENDED, user.isSuspended());
	}
}
