# Auth0

Auth0 handles authentication for both the backend (`./zenobase`) and the frontend (`./zenobase-web`). It is configured manually via the Auth0 dashboard — not managed by Pulumi. This directory documents the pieces that live in Auth0 and tracks the source of any custom code we deploy there.

## Components

- **Tenant + custom domain.** Configured in `application.yaml` via `auth0.domain`, `auth0.audience`, and `auth0.jwks_domain`. Custom domains require a separate `auth0.m2m.domain` for Management API access (the API v2 endpoint runs on the canonical tenant domain).
- **Database connection.** Stores users and password hashes.
- **SPA application.** Used by the frontend (`@auth0/auth0-vue`). Universal Login handles sign-in, sign-up, and password reset UI.
- **M2M application.** Used by the backend's `Auth0ManagementService` for email updates and passkey listing/deletion. Required scopes: `read:users`, `update:users`, `delete:users`, `read:authentication_methods`, `delete:authentication_methods`.
- **Post-login actions.** Two actions in the login flow:
    - [actions/set-claims.js](actions/set-claims.js) — adds the custom claims the backend reads from the access token. Always attached.
    - [actions/force-password-reset.js](actions/force-password-reset.js) — for users whose password pre-dates the configured cutoff, triggers a password-reset email and denies the current login attempt; the user follows the link in their inbox to set a new password and then signs in normally. Attached only when a rotation is in progress (migration, policy change, etc.); detach or remove when no longer needed. Must run **before** set-claims in the flow. Configure the post-reset landing page in **Branding → Email Templates → Change Password → Redirect To**.

## Editing the post-login Actions

Source of truth for each Action is the Auth0 dashboard (Actions → Library → Custom), but we keep a copy in [actions/](actions/) so changes are reviewable.

To deploy a change:

1. Edit the file in [actions/](actions/) and commit.
2. Open the matching Action in the Auth0 dashboard, paste the updated source, and click **Deploy**.
3. Confirm the Action is attached to the Login flow (Actions → Flows → Login) in the right order: `force-password-reset` (if attached) before `set-claims`.

Neither Action needs secrets. The reset cutoff in `force-password-reset` is hardcoded at the top of the file — bump it when a rotation event happens, paste, and **Deploy**. The reset email itself is sent via the public Authentication API (`/dbconnections/change_password`), so no Management API credentials are involved.
