package com.zenobase.json;

import java.math.BigDecimal;
import java.util.Objects;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Measures;
import com.zenobase.search.ExistsConstraintBuilder;
import com.zenobase.search.MeasureConstraintBuilder;
import com.zenobase.search.MeasureRangeConstraintBuilder;
import com.zenobase.search.TermConstraintBuilder;

public class DecimalMeasureField<Q extends Quantity> extends Field<DecimalMeasure<Q>> {

	public static final DecimalField VALUE = new DecimalField("@value");
	public static final TokenField UNIT = new TokenField("unit");
	public static final DecimalField VALUE_SI = new DecimalField("_value");

	public DecimalMeasureField(String name) {
		super(name, DecimalMeasure.class.getGenericSuperclass(), "object");
		addConstraintBuilder(name, new ExistsConstraintBuilder(getPath()));
		addConstraintBuilder(name, new MeasureRangeConstraintBuilder(getPath()));
		addConstraintBuilder(name, new MeasureConstraintBuilder(getPath()));
		addConstraintBuilder(concat(name, UNIT.getName()), new TermConstraintBuilder(concat(name, UNIT.getName())));
	}

	@Override
	public String getPathForSorting() {
		return concat(getPath(), VALUE_SI.getName());
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
	protected @Nullable DecimalMeasure<Q> getValue(JsonNode node) {
		return getDecimalMeasure((ObjectNode) node);
	}

	private @Nullable DecimalMeasure<Q> getDecimalMeasure(ObjectNode node) {
		return Measures.valueOf(
				Objects.requireNonNull(VALUE.getValue(node)), Objects.requireNonNull(UNIT.getValue(node)));
	}

	@Override
	public JsonNode toJson(@Nullable DecimalMeasure<Q> value) {
		return value != null ? toJson(value.getValue(), value.getUnit()) : NullNode.getInstance();
	}

	private static JsonNode toJson(BigDecimal value, Unit<?> unit) {
		Preconditions.checkNotNull(value);
		Preconditions.checkNotNull(unit);
		ObjectNode node = Nodes.newObject();
		VALUE.setValue(node, value);
		UNIT.setValue(node, unit.toString());
		return node;
	}

	@Override
	public void prePersist(ObjectNode node) {
		for (JsonNode childNode : getNodes(node)) {
			ObjectNode fieldNode = (ObjectNode) childNode;
			DecimalMeasure<Q> value = Objects.requireNonNull(getDecimalMeasure(fieldNode));
			VALUE_SI.setValue(fieldNode, Measures.toStandard(value).getValue());
		}
	}

	@Override
	public void postPersist(ObjectNode node) {
		for (JsonNode childNode : getNodes(node)) {
			ObjectNode fieldNode = (ObjectNode) childNode;
			VALUE_SI.setValue(fieldNode, null);
		}
	}
}
