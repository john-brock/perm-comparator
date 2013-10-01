package unit;
import org.junit.*;

import controllers.CompareUtils.CompareUserPerms;
import controllers.CompareUtils.BaseCompare;

import java.util.*;
import play.test.*;
import models.*;

public class UserPermissionCompareTest extends BaseUnitTest {
	PermissionSet permset1;
	PermissionSet permset2;
	Set<String> user1Perms = new HashSet<String>(Arrays.asList(new String[] {"PermissionsApiEnabled", "PermissionsAuthorApex"}));
	Set<String> user2Perms = new HashSet<String>(Arrays.asList(new String[] {"PermissionsApiEnabled", "PermissionsManageCases"}));

	@Before
	public void setUp() {
		permset1 = new PermissionSet("permset1");
		permset1.setUserPerms(user1Perms);

		permset2 = new PermissionSet("permset2");
    	permset2.setUserPerms(user2Perms);

    	PermissionSet[] permsets = new PermissionSet[] {permset1, permset2};
		CompareUserPerms.classifyUserPerms(permsets);
	}
	
    @Test
    public void testCommonUserPerms() {
    	checkPerms(permset1, new HashSet<String>(Arrays.asList(new String[] {"PermissionsApiEnabled"})), permset1.getCommonUserPerms());
    	checkPerms(permset2, new HashSet<String>(Arrays.asList(new String[] {"PermissionsApiEnabled"})), permset2.getCommonUserPerms()); 
    }
    
    @Test
    public void testUniqueUserPerms() {
    	checkPerms(permset1, new HashSet<String>(Arrays.asList(new String[] {"PermissionsAuthorApex"})), permset1.getUniqueUserPerms());
    	checkPerms(permset2, new HashSet<String>(Arrays.asList(new String[] {"PermissionsManageCases"})), permset2.getUniqueUserPerms());
    }
    
    @Test
    public void testDifferingUserPerms() {
    	checkPerms(permset1, new HashSet<String>(Arrays.asList(new String[] {"PermissionsAuthorApex"})), permset1.getDifferenceUserPerms());
    	checkPerms(permset2, new HashSet<String>(Arrays.asList(new String[] {"PermissionsManageCases"})), permset2.getDifferenceUserPerms());
    }
    
    public void checkCommonPerms(Set<String> expectedCommonPerms, PermissionSet... permsets) {
		for (PermissionSet permset : permsets) {
			Set<String> actualCommonPerms = permset.getCommonUserPerms();
			checkPerms(permset, expectedCommonPerms, actualCommonPerms);
		}
    }
}
