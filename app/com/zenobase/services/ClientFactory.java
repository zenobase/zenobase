package com.zenobase.services;

import org.opensearch.client.RestHighLevelClient;

public interface ClientFactory {

	RestHighLevelClient createClient();
}
