/**
 * Auth0 post-login Action: force users with an out-of-date password to
 * reset before they can finish signing in. A user needs to reset if their
 * last_password_reset is unset OR earlier than the cutoff hardcoded below.
 * Use cases include bulk-imported passwords from a migration and forced
 * rotation after a password-policy change. While a reset is needed, we
 * trigger a password-reset email via the Authentication API and deny the
 * current login attempt; the user follows the link in their inbox to set
 * a new password (which also proves they control the email — stronger
 * than letting them set a new password in-session). After the reset they
 * land on the URL configured in Branding -> Email Templates -> Change
 * Password -> Redirect To and sign in normally with the new password.
 *
 * Deploy via the Auth0 dashboard (Actions -> Library -> Custom), attach to
 * the Login flow ahead of set-claims.js, and bind the Database connection
 * only. Detach or remove this Action when the reset gate is no longer
 * needed.
 *
 * Fails open: an Authentication API error logs to console and lets the
 * login proceed. Forcing a reset isn't critical enough to lock users out
 * over a transient outage.
 */
exports.onExecutePostLogin = async (event, api) => {
	// Bump this when a rotation event happens (migration, policy change).
	const minResetDate = new Date("2026-05-06T00:00:00Z");

	// Only Database-connection users have a password to rotate, and emailing a
	// reset link is pointless for refresh-token grants where no human is present.
	if (event.connection.strategy !== "auth0") return;
	if (!event.transaction?.redirect_uri) return;

	const lastReset = event.user.last_password_reset ? new Date(event.user.last_password_reset) : null;
	if (lastReset && lastReset >= minResetDate) {
		return;
	}

	if (await sendPasswordResetEmail(event)) {
		api.access.deny("Your password has expired. Please check your email for a link to reset it.");
	}
	// If the email couldn't be sent, fall through and let the login complete normally
	// — the error is in the Action logs and forcing a reset isn't worth locking users out over.
};

async function sendPasswordResetEmail(event) {
	// Canonical tenant domain — same approach as Management API calls.
	const tenantDomain = `${event.tenant.id}.us.auth0.com`;
	const res = await fetch(`https://${tenantDomain}/dbconnections/change_password`, {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({
			client_id: event.client.client_id,
			email: event.user.email,
			connection: event.connection.name,
		}),
	});
	if (!res.ok) {
		console.error(`force-password-reset: change_password ${res.status}: ${await res.text()}`);
		return false;
	}
	return true;
}
