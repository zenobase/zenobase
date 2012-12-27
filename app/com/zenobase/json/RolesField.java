package com.zenobase.json;

import com.zenobase.models.Identity;
import com.zenobase.models.Role;

public class RolesField extends MapField<Identity, Role> {

	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final EnumField<Role> ROLE = EnumField.newInstance("role", Role.class);

	public RolesField(String name) {
		super(name, PRINCIPAL, ROLE);
		addConstraintBuilders(PRINCIPAL);
		addConstraintBuilders(ROLE);
	}
}
