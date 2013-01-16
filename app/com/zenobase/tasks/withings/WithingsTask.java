package com.zenobase.tasks.withings;

import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.common.Measures;
import com.zenobase.json.IntegerField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthTask;

public class WithingsTask extends OAuthTask {

	public static final String TYPE = "withings";
	public static final IntegerField USER_ID = new IntegerField("userId");
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField UNIT = new TokenField("unit");

	public WithingsTask(ObjectNode node) {
		super(node);
	}

	WithingsTask(String bucketId, Identity principal, Token token, Integer userId, String marker) {
		super(TYPE, bucketId, principal, token);
		setCredential(USER_ID, userId);
		setMarker(marker);
	}

	public Integer getUserId() {
		return getCredential(USER_ID);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public void setTag(String tag) {
		setSetting(TAG, tag);
	}

	public Unit<Mass> getUnit() {
		return Measures.<Mass>parseUnit(getSetting(UNIT));
	}

	public void setUnit(Unit<Mass> unit) {
		setSetting(UNIT, unit.toString());
	}

	@Override
	public WithingsTask copy() {
		return copy(getClass());
	}
}
