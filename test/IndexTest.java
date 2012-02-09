import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexTest {

	private Node node;
	private Client client;

	@Before
	public void setUp() throws Exception {
		Settings settings = ImmutableSettings.settingsBuilder()
				.put("node.http.enabled", false)
				.put("index.gateway.type", "none")
				.put("index.store.type", "memory")
				.put("index.number_of_shards", 1)
				.put("index.number_of_replicas", 0).build();
		node = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
		client = node.client();
	}

	@Test
	public void test() throws Exception {

		String indexName = "test";
		String typeName = "post";
		String docId = "1";
		String fieldName = "user";
		String fieldValue = "me";

		createIndex(indexName);
		putMapping(indexName, typeName, buildMapping(fieldName));
		put(indexName, typeName, docId, buildObject(fieldValue));
		Map<String, Object> result = get(indexName, typeName, docId);

		Assert.assertEquals(fieldName + " in " + result, fieldValue, result.get(fieldName));
		Assert.assertEquals("_index in " + result, indexName, result.get("_index"));
	}

	private void createIndex(String indexName) throws Exception {
		client.admin().indices().prepareCreate(indexName).execute().actionGet();
	}

	private void putMapping(String indexName, String typeName, XContentBuilder mapping) {
		client.admin().indices().preparePutMapping(indexName).setType(typeName).setSource(mapping).execute().actionGet();
	}

	private XContentBuilder buildMapping(String fieldName) throws Exception {
		XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject();
		mappingBuilder.startObject("_index").field("enabled", true);
		XContentBuilder propertiesBuilder = mappingBuilder.startObject("properties");
		propertiesBuilder.startObject(fieldName).field("type", "string");
		System.out.println("Mapping: " + mappingBuilder.string());
		return mappingBuilder;
	}

	private Map<String, Object> buildObject(String user) {
		Map<String, Object> object = new HashMap<String, Object>();
		object.put("user", user);
		return object;
	}

	private void put(String indexName, String typeName, String docId, Map<String, Object> source) {
		client.prepareIndex(indexName, typeName, docId).setSource(source).execute().actionGet();
	}

	private Map<String, Object> get(String indexName, String typeName, String docId) {
		return client.prepareGet(indexName, typeName, docId).execute().actionGet().sourceAsMap();
	}

	@After
	public void tearDown() {
		client.close();
		node.close();
	}
}
