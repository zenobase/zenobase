package com.zenobase.search;

import java.util.Arrays;
import java.util.List;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.range.RangeFacet;
import org.elasticsearch.search.facet.range.RangeFacetBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.json.Field;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class PhasesFacet extends Facet {

	public static final String TYPE = "phases";

	private final String keyField;
	private final String valueField;
	private final Unit<?> unit;
	private final int phases;
	private final FilterBuilder filter;

	public PhasesFacet(String id, String keyField, String valueField, Unit<?> unit, int phases, FilterBuilder filter) {
		super(id);
		Preconditions.checkArgument(phases < 100, "too many phases: " + phases);
		this.keyField = keyField;
		this.valueField = valueField;
		this.unit = unit;
		this.phases = phases;
		this.filter = filter;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		RangeFacetBuilder facet = FacetBuilders.rangeFacet(getId()).keyField(keyField)
			.valueField(unit == Unit.ONE ? valueField : Field.concat(valueField, MeasurementField.VALUE_SI.getName()));
		for (double from = 0.0; from < 1.0; from += getInterval()) {
			facet.addRange(from, Math.min(from + getInterval(), 1.0));
		}
		facet.facetFilter(filter);
		builder.facet(facet);
	}

	private double getInterval() {
		return 1.0 / phases;
	}

	@Override
	public JsonNode process(SearchResponse response) {
		RangeFacet facet = response.getFacets().facet(RangeFacet.class, getId());
		List<RangeFacet.Entry> entries = Arrays.asList(new RangeFacet.Entry[phases]);
		for (RangeFacet.Entry entry : Lists.reverse(facet.getEntries())) {
			entries.set((int) (entry.getFrom() * phases), entry);
		}
		return toJson(entries);
	}

	private JsonNode toJson(List<RangeFacet.Entry> entries) {
		ArrayNode node = Nodes.newArray();
		int total = 0;
		for (int i = 0; i < entries.size(); ++i) {
			ObjectNode entryNode = Nodes.newObject();
			entryNode.put("label", (i + 1) + "/" + phases);
			entryNode.put("from", i * getInterval());
			entryNode.put("to", (i + 1) * getInterval());
			RangeFacet.Entry entry = entries.get(i);
			if (entry != null) {
				total += entry.getCount();
				entryNode.put("count", entry.getCount());
				if (!keyField.equals(valueField) && entry.getTotalCount() > 0) {
					addValue(entryNode, "min", entry.getMin());
					addValue(entryNode, "max", entry.getMax());
					addValue(entryNode, "sum", entry.getTotal());
					addValue(entryNode, "avg", entry.getMean());
				}
			} else {
				entryNode.put("count", 0);
			}
			node.add(entryNode);
		}
		return total > 0 ? node : Nodes.newArray();
	}

	private void addValue(ObjectNode parent, String property, double value) {
		if (unit != Unit.ONE) {
			ObjectNode node = parent.putObject(property);
			node.put("@value", Measures.convert(value, unit));
			node.put("unit", unit.toString());
		} else {
			parent.put(property, Measures.round(value));
		}
	}

	public static FacetBuilder builder(final FilterParser filterParser) {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				String unit = options.get("unit");
				return new PhasesFacet(
					options.get("id"),
					options.get("key_field", String.class, Event.PHASE.getName()),
					options.get("value_field", String.class, Event.PHASE.getName()),
					unit != null ? Measures.parseUnit(unit) : Unit.ONE,
					options.get("phases", Integer.class, 8),
					filterParser.parse(options.get("filter")));
			}
		};
	}
}
