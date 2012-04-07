package schema;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import common.Measures;
import common.Nodes;

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

	private static void configureSchema(ObjectNode properties, Field<?> field) {
		field.configureSchema(properties.putObject(field.getName()));
	}

	@Override
	protected DecimalMeasure<Q> getValue(JsonNode node) {
		return getDecimalMeasure((ObjectNode) node);
	}

	private DecimalMeasure<Q> getDecimalMeasure(ObjectNode object) {
		return DecimalMeasure.valueOf(VALUE.getValue(object),
			(Unit<Q>) Unit.valueOf(UNIT.getValue(object)));
	}

	@Override
	protected JsonNode toJson(DecimalMeasure<Q> value) {
		ObjectNode object = Nodes.newObject();
		VALUE.setValue(object, value.getValue());
		UNIT.setValue(object, value.getUnit().toString());
		return object;
	}

	@Override
	public void prePersist(ObjectNode object) {
		for (JsonNode node : getNodes(object)) {
			ObjectNode fieldNode = (ObjectNode) node;
			DecimalMeasure<Q> value = getDecimalMeasure(fieldNode);
			VALUE_SI.setValue(fieldNode, Measures.toStandard(value).getValue());
		}
	}
}
