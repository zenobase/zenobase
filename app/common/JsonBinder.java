package common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import play.data.binding.Global;
import play.data.binding.TypeBinder;

@Global
public class JsonBinder implements TypeBinder<ObjectNode> {

	private static final ObjectMapper mapper = new ObjectMapper();

	public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws Exception {
		return (ObjectNode) mapper.readTree(value);
	}
}
