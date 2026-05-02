package com.zenobase.json;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.common.Generator;
import com.zenobase.common.Units;
import com.zenobase.repositories.Index;
import com.zenobase.repositories.OpenSearchTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class FieldTestSupport<T> extends OpenSearchTestSupport {

	private static final String INDEX_NAME = "index";
	private static final String TYPE_NAME = "type";
	private static final String FIELD_NAME = "field";

	private Field<T> field;
	private Index index;

	@BeforeAll
	public static void initUnits() {
		Units.isMetric(Units.M);
	}

	@BeforeEach
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
		index.store(id, node, true);
		field.postPersist(node);
		ObjectNode foundNode = index.get(id);
		foundNode.remove(DomainNode.SEQ_NO_FIELD);
		foundNode.remove(DomainNode.PRIMARY_TERM_FIELD);
		assertThat(foundNode).isEqualTo(node);
	}

	@AfterEach
	public void tearDown() {
		index.close();
	}
}
