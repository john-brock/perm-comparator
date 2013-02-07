package unit;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import models.PermissionSet;
import play.test.UnitTest;

public class BaseUnitTest extends UnitTest {
    
	public <T> void checkPerms(Set<T> expectedPerms, Set<T> actualPerms) {
		checkPerms(null, expectedPerms, actualPerms);
    }
	
	public <T> void checkPerms(PermissionSet permset, Set<T> expectedPerms, Set<T> actualPerms) {
		assertEquals("Size of perm enumSet not expected" + ((permset == null) ? "." : 
			String.format(" for permset %s.", permset.getId())) + String.format("Expected set %s but actual " +
				"was %s.", expectedPerms.toString(), actualPerms.toString()), 
			expectedPerms.size(), actualPerms.size());
		
		for (T perm : expectedPerms) {
			assertTrue(String.format("Did not have expected perm %s", perm.toString()) + ((permset == null) ? 
					"." : String.format(" for %s permset.", permset.getId())),
					actualPerms.contains(perm));
		}
    }
	
//    public <T> void checkCommonPerms(Set<T> expectedCommonPerms, PermissionSet... permsets) {
//		for (PermissionSet permset : permsets) {
//			Set<T> actualCommonPerms = permset.getCommonUserPerms();
//			checkPerms(permset, expectedCommonPerms, actualCommonPerms);
//		}
//    }
	
	@Test
	public void dummyTest() {
		assertTrue(true);
	}
}
