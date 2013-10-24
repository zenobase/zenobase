package com.zenobase.json;

import com.zenobase.models.Identity;
import com.zenobase.models.Role;

public class RolesField extends MapField<Identity, Role> {

	public static final String PRINCIPAL = "principal";
	public static final String ROLE = "role";

	public RolesField(String name) {
		super(name);
	}

	@Override
	protected Field<Identity> getKeyField() {
		return new IdentityField(PRINCIPAL, this);
	}

	@Override
	protected Field<Role> getValueField() {
		return EnumField.newInstance(ROLE, Role.class, this);
	}
}
