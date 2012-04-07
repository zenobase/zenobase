package schema;

import secure.Identity;
import secure.Permission;

public class PermissionField extends MapField<Identity, Permission> {

	public static final IdentityField IDENTITY = new IdentityField("identity");
	public static final EnumField<Permission> PERMISSION = new EnumField<Permission>("permission", Permission.class);

	public PermissionField(String name) {
		super(name, IDENTITY, PERMISSION);
	}
}
