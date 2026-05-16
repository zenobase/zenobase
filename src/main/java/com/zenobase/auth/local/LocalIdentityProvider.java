package com.zenobase.auth.local;

import com.zenobase.auth.IdentityProvider;
import com.zenobase.auth.Passkey;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import java.util.List;
import org.jspecify.annotations.Nullable;

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

	@Override
	public void deleteApplication(Identity application) {}

	@Override
	public @Nullable String getApplicationName(Identity application) {
		return null;
	}
}
