package com.zenobase.auth.local;

import com.zenobase.auth.UserDirectory;
import com.zenobase.models.User;

public class LocalUserDirectory implements UserDirectory {

	@Override
	public void updateEmail(User user, String newEmail) {}

	@Override
	public void deleteUser(User user) {}
}
