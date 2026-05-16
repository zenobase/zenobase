package com.zenobase.auth;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface IdentityProvider {
	void updateEmail(User user, String newEmail);

	void deleteUser(User user);

	List<Passkey> listPasskeys(User user);

	void deletePasskey(User user, String passkeyId);

	void deleteApplication(Identity client);

	@Nullable
	String getApplicationName(Identity client);
}
