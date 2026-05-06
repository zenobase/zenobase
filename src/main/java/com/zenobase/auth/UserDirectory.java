package com.zenobase.auth;

import com.zenobase.models.User;
import java.util.List;

public interface UserDirectory {
	void updateEmail(User user, String newEmail);

	void deleteUser(User user);

	List<Passkey> listPasskeys(User user);

	void deletePasskey(User user, String passkeyId);
}
