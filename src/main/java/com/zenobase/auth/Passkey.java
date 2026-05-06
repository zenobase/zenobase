package com.zenobase.auth;

import org.jspecify.annotations.Nullable;

public record Passkey(
	String id,
	@Nullable String name,
	String createdAt,
	@Nullable String lastAuthAt,
	@Nullable String userAgent
) {}
