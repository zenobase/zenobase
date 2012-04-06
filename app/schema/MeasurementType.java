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

public class MeasurementType<Q extends Quantity> extends Type<DecimalMeasure<Q>> {

	private static final Field<BigDecimal> VALUE = Field.of("@value", new DecimalType());
	private static final Field<String> UNIT = Field.of("unit", new TokenType());
	private static final Field<BigDecimal> VALUE_SI = Field.of("value", new DecimalType());

	public MeasurementType() {
		super(DecimalMeasure.class.getGenericSuperclass(), "object");
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
		field.getType().configureSchema(properties.putObject(field.getName()));
	}

	@Override
	protected DecimalMeasure<Q> get(JsonNode node) {
		return get((ObjectNode) node);
	}

	private static <Q extends Quantity> DecimalMeasure<Q> get(ObjectNode object) {
		BigDecimal value = get(object, VALUE);
		Unit<Q> unit = (Unit<Q>) Unit.valueOf(get(object, UNIT));
		return DecimalMeasure.valueOf(value, unit);
	}

	private static <T> T get(JsonNode node, Field<T> field) {
		return Iterables.getOnlyElement(field.getType().get((ObjectNode) node, field.getName()));
	}

	@Override
	protected JsonNode get(DecimalMeasure<Q> value) {
		ObjectNode object = Nodes.newObject();
		add(object, VALUE, value.getValue());
		add(object, UNIT, value.getUnit().toString());
		return object;
	}

	private static <T> void add(ObjectNode object, Field<T> field, T value) {
		field.getType().add(object, field.getName(), value);
	}

	@Override
	public void prePersist(ObjectNode object, String fieldName) {
		ObjectNode fieldNode = (ObjectNode) object.get(fieldName); // TODO: hande arrays!
		DecimalMeasure<Q> value = get(fieldNode);
		add(fieldNode, VALUE_SI, Measures.toStandard(value).getValue());
	}

	@Override
	public void postLoad(ObjectNode object, String fieldName) {
		ObjectNode fieldNode = (ObjectNode) object.get(fieldName); // TODO: hande arrays!
		if (fieldNode != null) {
			fieldNode.remove(VALUE_SI.getName());
		}
	}
}
