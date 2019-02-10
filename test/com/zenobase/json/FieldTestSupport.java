package com.zenobase.json;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.zenobase.common.Generator;
import com.zenobase.common.Units;
import com.zenobase.services.ElasticSearchTestSupport;
import com.zenobase.services.Index;

public abstract class FieldTestSupport<T> extends ElasticSearchTestSupport {

	private static final String INDEX_NAME = "index";
	private static final String TYPE_NAME = "type";
	private static final String FIELD_NAME = "field";

	private Field<T> field;
	private Index index;

	@BeforeClass
	public static void initUnits() {
		Units.isMetric(Units.M);
	}

	@Before
	public void setUp() {
		field = newField(FIELD_NAME);
		index = getManager().getIndex(INDEX_NAME);
		index.create(0);
		index.putMapping(new SchemaBuilder(TYPE_NAME).add(field).build());
	}

	protected abstract Field<T> newField(String name);

	protected void roundtrip(T value) {
		ObjectNode node = Nodes.newObject();
		field.setValue(node, value);
		field.prePersist(node);
		String id = Generator.id();
		index.store(TYPE_NAME, id, node, DateTime.now(), true);
		field.postPersist(node);
		ObjectNode foundNode = index.get(TYPE_NAME, id);
		assertThat(Nodes.readObject(foundNode.toString())).isEqualTo(Nodes.readObject(node.toString())); // TODO investigate why some tests fail if we don't reparse the json
	}

	@After
	public void tearDown() {
		index.close();
	}
}
