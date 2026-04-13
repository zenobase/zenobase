package com.zenobase.auth;

import com.zenobase.models.User;

public interface UserDirectory {

	void updateEmail(User user, String newEmail);

	void deleteUser(User user);
}
