package com.zenobase.auth.local;

import com.zenobase.auth.Passkey;
import com.zenobase.auth.UserDirectory;
import com.zenobase.models.User;
import java.util.List;

public class LocalUserDirectory implements UserDirectory {

	@Override
	public void updateEmail(User user, String newEmail) {}

	@Override
	public void setSuspended(User user, boolean suspended) {}

	@Override
	public void deleteUser(User user) {}

	@Override
	public List<Passkey> listPasskeys(User user) {
		return List.of();
	}

	@Override
	public void deletePasskey(User user, String passkeyId) {}
}
