/**
 * Model Context Protocol (MCP) server — exposes Zenobase buckets as MCP Resources and event queries as MCP Tools so
 * LLM clients (Claude Desktop, Cursor, etc.) can analyze a user's data over a standard protocol. Authenticated via
 * Auth0 with the {@code auth0.external_audience} token, gated per-bucket by an
 * {@link com.zenobase.repositories.ExternalBucketGrantRepository external bucket grant} the user issues from the
 * Connected Apps page in the web UI.
 */
@NullMarked
package com.zenobase.mcp;

import org.jspecify.annotations.NullMarked;
