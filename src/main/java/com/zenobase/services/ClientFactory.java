package com.zenobase.services;

import org.opensearch.client.opensearch.OpenSearchClient;

public interface ClientFactory {
	OpenSearchClient createClient();
}
