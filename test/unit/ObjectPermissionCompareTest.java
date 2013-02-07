package unit;
import org.junit.*;

import controllers.PermissionSetUtil;

import java.util.*;

import play.Logger;
import play.test.*;
import models.*;
import models.PermissionSet.ObjPermCategory;
import models.PermissionSet.objectPermissions;

public class ObjectPermissionCompareTest extends BaseUnitTest {
	private PermissionSet permset1;
	private PermissionSet permset2;
	
	private Map<String, EnumSet<objectPermissions>> obj1Map;
	private Map<String, EnumSet<objectPermissions>> obj2Map;

	private final EnumSet<objectPermissions> obj1Perms = EnumSet.of(objectPermissions.PermissionsRead, objectPermissions.PermissionsCreate);
	private final EnumSet<objectPermissions> obj2Perms = EnumSet.of(objectPermissions.PermissionsRead, objectPermissions.PermissionsEdit);
	
	private static final String obj1 = "Object1";
	private static final String obj2 = "Object2";
	
	private PermissionSet[] permsets;
	
	@Before
	public void setUp() {
		permset1 = new PermissionSet("permset1");
		obj1Map = new HashMap();
		obj1Map.put(obj1, obj1Perms.clone());
		permset1.setObjPermMap(ObjPermCategory.original, obj1Map);
		
		permset2 = new PermissionSet("permset2");
		obj2Map = new HashMap();
		obj2Map.put(obj2, obj2Perms.clone());
		permset2.setObjPermMap(ObjPermCategory.original, obj2Map);
		
    	permsets = new PermissionSet[] {permset1, permset2};
	}
	
	@Test
	public void testOnePermset() {
		PermissionSetUtil.classifyObjectPerms(new PermissionSet[] {permset1});
		
    	Map<String, EnumSet<objectPermissions>> permsetObjPermMap = permset1.getOjPermMap(ObjPermCategory.common);
    	checkObjPermKeys(permsetObjPermMap, obj1Map);
    	checkPerms(permset1, obj1Perms, permsetObjPermMap.get(obj1));
    	
    	permsetObjPermMap = permset1.getOjPermMap(ObjPermCategory.unique);
    	checkObjPermKeys(permsetObjPermMap, obj1Map);
    	checkPerms(permset1, obj1Perms, permsetObjPermMap.get(obj1));
    	
    	permsetObjPermMap = permset1.getOjPermMap(ObjPermCategory.differing);
    	checkObjPermKeys(permsetObjPermMap, new HashMap<String, EnumSet<objectPermissions>>());
	}
	
    @Test
    public void testNoCommonObjCommonPerms() {
		PermissionSetUtil.classifyObjectPerms(permsets);

		Map<String, EnumSet<objectPermissions>> permset1Map = permset1.getOjPermMap(ObjPermCategory.common);
		Map<String, EnumSet<objectPermissions>> permset2Map = permset2.getOjPermMap(ObjPermCategory.common);
    	
		Map<String, EnumSet<objectPermissions>> emptyMap = new HashMap();

		// no common perms --> no 'keys' in the common obj perm map
		checkObjPermKeys(permset1Map, emptyMap);
		checkObjPermKeys(permset2Map, emptyMap);
    }
    
    @Test
    public void testCommonPerms() {
		obj1Map.put(obj2, obj2Perms.clone());
		permset1.setObjPermMap(ObjPermCategory.original, obj1Map);
		
		PermissionSetUtil.classifyObjectPerms(permsets);

		Map<String, EnumSet<objectPermissions>> permset1Map = permset1.getOjPermMap(ObjPermCategory.common);
		Map<String, EnumSet<objectPermissions>> permset2Map = permset2.getOjPermMap(ObjPermCategory.common);
    	
		Map<String, EnumSet<objectPermissions>> commonMap = new HashMap();
		commonMap.put(obj2, obj2Perms);  // common between permset1 and permset2
		
		checkObjPermKeys(permset1Map, commonMap);
		checkObjPermKeys(permset2Map, commonMap);
    }
    
    @Test
    public void testNoCommonObjUniquePerms() {
		PermissionSetUtil.classifyObjectPerms(permsets);

    	Map<String, EnumSet<objectPermissions>> permset1Map = permset1.getOjPermMap(ObjPermCategory.unique);
    	Map<String, EnumSet<objectPermissions>> permset2Map = permset2.getOjPermMap(ObjPermCategory.unique);
    	
    	checkObjPermKeys(permset1Map, obj1Map);
    	checkObjPermKeys(permset2Map, obj2Map);
    	
    	checkPerms(permset1, obj1Perms, permset1Map.get(obj1));
    	checkPerms(permset2, obj2Perms, permset2Map.get(obj2));
    }
    
    @Test
    public void testUniquePerms() {
		obj1Map.put(obj2, obj2Perms.clone());
		permset1.setObjPermMap(ObjPermCategory.original, obj1Map);
		
		PermissionSetUtil.classifyObjectPerms(permsets);

    	Map<String, EnumSet<objectPermissions>> permset1map = permset1.getOjPermMap(ObjPermCategory.unique);
    	Map<String, EnumSet<objectPermissions>> permset2map = permset2.getOjPermMap(ObjPermCategory.unique);

    	obj1Map.remove(obj2);  // obj2 is common between permset1 and permset2
    	checkObjPermKeys(permset1map, obj1Map);
    	checkObjPermKeys(permset2map, new HashMap<String, EnumSet<objectPermissions>>());  // no keys (no unique)
    	
    	checkPerms(permset1, obj1Perms, permset1map.get(obj1));
    }
    
    @Test
    public void testUniqueSpecificPerm() {
		objectPermissions sharedPerm = objectPermissions.PermissionsRead;
    	obj1Map.put(obj2, EnumSet.of(sharedPerm));
		permset1.setObjPermMap(ObjPermCategory.original, obj1Map);
		
		PermissionSetUtil.classifyObjectPerms(permsets);

    	Map<String, EnumSet<objectPermissions>> permset1map = permset1.getOjPermMap(ObjPermCategory.unique);
    	Map<String, EnumSet<objectPermissions>> permset2map = permset2.getOjPermMap(ObjPermCategory.unique);

    	obj1Map.remove(obj2);  // obj2 is common between permset1 and permset2
    	checkObjPermKeys(permset1map, obj1Map);
    	checkObjPermKeys(permset2map, obj2Map);  // has common obj2 perm, but also 1 unique perm
    	
    	checkPerms(permset1, obj1Perms, permset1map.get(obj1));
    	obj2Perms.remove(sharedPerm);  // unique perm is obj2 edit
    	checkPerms(permset2, obj2Perms, permset2map.get(obj2));
    }
    
    @Test
    public void testNoCommonObjDifferingPerms() {
		PermissionSetUtil.classifyObjectPerms(permsets);

    	Map<String, EnumSet<objectPermissions>> permset1map = permset1.getOjPermMap(ObjPermCategory.differing);
    	Map<String, EnumSet<objectPermissions>> permset2map = permset2.getOjPermMap(ObjPermCategory.differing);

    	checkObjPermKeys(permset1map, obj1Map);
    	checkObjPermKeys(permset2map, obj2Map);

    	checkPerms(permset1, obj1Perms, permset1map.get(obj1));
    	checkPerms(permset2, obj2Perms, permset2map.get(obj2));
    }
    
    @Test
    public void testDifferingPerms() {
		obj1Map.put(obj2, obj2Perms.clone());
		permset1.setObjPermMap(ObjPermCategory.original, obj1Map);
		
    	PermissionSetUtil.classifyObjectPerms(permsets);

    	Map<String, EnumSet<objectPermissions>> permset1map = permset1.getOjPermMap(ObjPermCategory.differing);
    	Map<String, EnumSet<objectPermissions>> permset2map = permset2.getOjPermMap(ObjPermCategory.differing);

    	obj1Map.remove(obj2);  // differing should not have a common object
    	checkObjPermKeys(permset1map, obj1Map);
    	checkObjPermKeys(permset2map, new HashMap<String, EnumSet<objectPermissions>>()); // should be empty map

    	checkPerms(permset1, obj1Perms, permset1map.get(obj1));
    }
    
    @Test
    public void testDifferingSpecificPerm() {
		objectPermissions sharedObj2Perm = objectPermissions.PermissionsRead;
    	obj1Map.put(obj2, EnumSet.of(sharedObj2Perm));
		permset1.setObjPermMap(ObjPermCategory.original, obj1Map);
		
    	PermissionSetUtil.classifyObjectPerms(permsets);

    	Map<String, EnumSet<objectPermissions>> permset1map = permset1.getOjPermMap(ObjPermCategory.differing);
    	Map<String, EnumSet<objectPermissions>> permset2map = permset2.getOjPermMap(ObjPermCategory.differing);

    	obj1Map.remove(obj2);  // differing should not have a common object
    	checkObjPermKeys(permset1map, obj1Map);
    	checkObjPermKeys(permset2map, obj2Map);  // should still contain obj2 key

    	checkPerms(permset1, obj1Perms, permset1map.get(obj1));
    	
    	obj2Perms.remove(sharedObj2Perm);  // should only have obj2 - PermissionsEdit
    	checkPerms(permset2, obj2Perms, permset2map.get(obj2));
    }
    
	private void checkObjPermKeys(Map<String, EnumSet<objectPermissions>> objPermMap, Map<String, EnumSet<objectPermissions>> expectedPermMap) {
		Set<String> actualKeys = objPermMap.keySet();
		Set<String> expectedKeys = expectedPermMap.keySet();
		
		assertTrue(String.format("ObjPermMap should contain %s key(s) but contained %s", expectedKeys.size(), actualKeys.size()), 
    			actualKeys.size() == expectedKeys.size());
    	for (String key : expectedKeys) {
    		assertTrue("ObjPermMap should have contained key " + key, actualKeys.contains(key));
    	}
	}
}
