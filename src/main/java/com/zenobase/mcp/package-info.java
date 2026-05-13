/**
 * Model Context Protocol (MCP) server — exposes Zenobase buckets as MCP Resources and event queries as MCP Tools so
 * LLM clients (Claude Desktop, Cursor, etc.) can analyze a user's data over a standard protocol. Authenticated via
 * Auth0 with the {@code auth0.external_audience} token, gated per-bucket by the {@code readable_buckets} field on the
 * {@link com.zenobase.models.ExternalClient external client record} the user manages from the API clients section of
 * Settings.
 */
@NullMarked
package com.zenobase.mcp;

import org.jspecify.annotations.NullMarked;
