package com.zenobase.schema;

import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class PermissionField extends MapField<Identity, Permission> {

	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final EnumField<Permission> PERMISSION = new EnumField<Permission>("permission", Permission.class);

	public PermissionField(String name) {
		super(name, PRINCIPAL, PERMISSION);
	}
}
