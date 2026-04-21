package com.zenobase.auth.local;

import com.zenobase.auth.UserDirectory;
import com.zenobase.models.Session;
import com.zenobase.models.User;
import java.util.List;

public class LocalUserDirectory implements UserDirectory {

	@Override
	public void updateEmail(User user, String newEmail) {}

	@Override
	public void deleteUser(User user) {}

	@Override
	public List<Session> listSessions(String externalId) {
		return List.of();
	}

	@Override
	public boolean revokeSession(String externalId, String sessionId) {
		return false;
	}

	@Override
	public boolean revokeAllSessions(String externalId) {
		return true;
	}
}
