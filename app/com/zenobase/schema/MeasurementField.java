package com.zenobase.schema;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.common.Nodes;

public class MeasurementField<Q extends Quantity> extends Field<DecimalMeasure<Q>> {

	public static final DecimalField VALUE = new DecimalField("@value");
	public static final TokenField UNIT = new TokenField("unit");
	public static final DecimalField VALUE_SI = new DecimalField("_value");

	public MeasurementField(String name) {
		super(name, DecimalMeasure.class.getGenericSuperclass(), "object");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, VALUE);
		configureSchema(properties, UNIT);
		configureSchema(properties, VALUE_SI);
	}

	@Override
	protected DecimalMeasure<Q> getValue(JsonNode node) {
		return getDecimalMeasure((ObjectNode) node);
	}

	private DecimalMeasure<Q> getDecimalMeasure(ObjectNode node) {
		return DecimalMeasure.valueOf(VALUE.getValue(node),
			(Unit<Q>) Unit.valueOf(UNIT.getValue(node)));
	}

	@Override
	protected JsonNode toJson(DecimalMeasure<Q> value) {
		ObjectNode node = Nodes.newObject();
		VALUE.setValue(node, value.getValue());
		UNIT.setValue(node, value.getUnit().toString());
		return node;
	}

	@Override
	public void prePersist(ObjectNode node) {
		for (JsonNode childNode : getNodes(node)) {
			ObjectNode fieldNode = (ObjectNode) childNode;
			DecimalMeasure<Q> value = getDecimalMeasure(fieldNode);
			VALUE_SI.setValue(fieldNode, Measures.toStandard(value).getValue());
		}
	}
}
