package com.zenobase.models;

import com.zenobase.json.DomainNode;
import com.zenobase.json.TokenField;

public class UserInfo extends DomainNode {

	private static final TokenField ID = new TokenField("@id");
	private static final TokenField NAME = new TokenField("name", false);

	public UserInfo(User user) {
		setValue(ID, user.getId());
		setValue(NAME, user.getName());
	}
}
