package com.zenobase.auth;

import com.zenobase.models.User;
import java.util.List;

public interface UserDirectory {
	void updateEmail(User user, String newEmail);

	void setSuspended(User user, boolean suspended);

	void deleteUser(User user);

	List<Passkey> listPasskeys(User user);

	void deletePasskey(User user, String passkeyId);
}
