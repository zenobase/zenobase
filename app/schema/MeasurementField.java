package schema;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.Iterables;
import common.Measures;
import common.Nodes;

public class MeasurementField<Q extends Quantity> extends Field<DecimalMeasure<Q>> {

	public static final DecimalField VALUE = new DecimalField("@value");
	public static final TokenField UNIT = new TokenField("unit");
	public static final DecimalField VALUE_SI = new DecimalField("_value_si");

	public MeasurementField(String name) {
		super(name, DecimalMeasure.class.getGenericSuperclass(), "object");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, VALUE); // TODO: no index no store
		configureSchema(properties, UNIT); // TODO: no index no store
		configureSchema(properties, VALUE_SI);
	}

	private static void configureSchema(ObjectNode properties, Field<?> field) {
		field.configureSchema(properties.putObject(field.getName()));
	}

	@Override
	protected DecimalMeasure<Q> getValue(JsonNode node) {
		return get((ObjectNode) node);
	}

	private static <Q extends Quantity> DecimalMeasure<Q> get(ObjectNode object) {
		BigDecimal value = Iterables.getOnlyElement(VALUE.getValues(object));
		Unit<Q> unit = (Unit<Q>) Unit.valueOf(Iterables.getOnlyElement(UNIT.getValues(object)));
		return DecimalMeasure.valueOf(value, unit);
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
		ObjectNode fieldNode = (ObjectNode) object.get(getName()); // TODO: hande arrays!
		DecimalMeasure<Q> value = get(fieldNode);
		VALUE_SI.setValue(fieldNode, Measures.toStandard(value).getValue());
	}

	@Override
	public void postLoad(ObjectNode object) {
		ObjectNode fieldNode = (ObjectNode) object.get(getName()); // TODO: hande arrays!
		if (fieldNode != null) {
			fieldNode.remove(VALUE_SI.getName());
		}
	}
}
