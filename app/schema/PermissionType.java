package schema;

import secure.Identity;
import secure.Permission;

public class PermissionType extends MapEntryType<Identity, Permission> {

	public static final Field<Identity> IDENTITY = Field.of("identity", new IdentityType());
	public static final Field<Permission> PERMISSION = Field.of("permission", new EnumType<Permission>(Permission.class));

	public PermissionType() {
		super(IDENTITY, PERMISSION);
	}
}
