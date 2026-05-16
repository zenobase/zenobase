package com.zenobase.auth.local;

import com.zenobase.auth.IdentityProvider;
import com.zenobase.auth.Passkey;
import com.zenobase.models.User;
import java.util.List;

public class LocalIdentityProvider implements IdentityProvider {

	@Override
	public void updateEmail(User user, String newEmail) {}

	@Override
	public void deleteUser(User user) {}

	@Override
	public List<Passkey> listPasskeys(User user) {
		return List.of();
	}

	@Override
	public void deletePasskey(User user, String passkeyId) {}
}
