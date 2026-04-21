package com.zenobase.auth;

import com.zenobase.models.Session;
import com.zenobase.models.User;
import java.util.List;

public interface UserDirectory {
	void updateEmail(User user, String newEmail);

	void deleteUser(User user);

	/** Lists active sessions for the user. Implementations without session tracking return {@link List#of()}. */
	List<Session> listSessions(String externalId);

	/**
	 * Revokes a single session (and its refresh token). Implementations must verify the session
	 * belongs to the user before revoking. Returns {@code true} if a revoke request was issued,
	 * {@code false} if the session is not owned by the user.
	 */
	boolean revokeSession(String externalId, String sessionId);

	/** Revokes all sessions and refresh tokens for the user. Returns {@code true} on success. */
	boolean revokeAllSessions(String externalId);
}
