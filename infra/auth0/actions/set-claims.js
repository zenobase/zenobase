/**
 * Auth0 post-login Action: add the custom claims the backend reads from
 * the access token (see Auth0TokenValidator.java).
 *
 * Deploy via the Auth0 dashboard (Actions -> Library -> Custom) and attach
 * to the Login flow. If force-password-reset.js is also attached, this
 * Action should run after it.
 */
exports.onExecutePostLogin = async (event, api) => {
	const namespace = "https://zenobase.com/";
	api.accessToken.setCustomClaim(namespace + "username", event.user.username);
	api.accessToken.setCustomClaim(namespace + "email", event.user.email);
	api.accessToken.setCustomClaim(namespace + "email_verified", event.user.email_verified);
};
