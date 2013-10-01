package unit;
import org.junit.*;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import controllers.CompareUtils.CompareObjectPerms;
import controllers.CompareUtils.BaseCompare;
import controllers.CompareUtils.CompareSetupEntityPerms;

import java.util.*;

import play.Logger;
import play.test.*;
import models.*;
import models.PermissionSet.ObjPermCategory;
import models.PermissionSet.SetupEntityTypes;
import models.PermissionSet.objectPermissions;

public class SEAPermissionCompareTest extends BaseUnitTest {
	private PermissionSet permset1;
	private PermissionSet permset2;
	private PermissionSet permset3; 
	private PermissionSet permset4; 

	private Map<ObjPermCategory, Map<SetupEntityTypes, Set<String>>> seaMap1 = Maps.newHashMap();
	private Map<ObjPermCategory, Map<SetupEntityTypes, Set<String>>> seaMap2 = Maps.newHashMap();
	private Map<ObjPermCategory, Map<SetupEntityTypes, Set<String>>> seaMap3 = Maps.newHashMap();
	private Map<ObjPermCategory, Map<SetupEntityTypes, Set<String>>> seaMap4 = Maps.newHashMap();

	private final Map<SetupEntityTypes, Set<String>> seaPerms1 = Maps.newHashMap();
	private final Map<SetupEntityTypes, Set<String>> seaPerms2 = Maps.newHashMap();
	private final Map<SetupEntityTypes, Set<String>> seaPerms3 = Maps.newHashMap();
	private final Map<SetupEntityTypes, Set<String>> seaPerms4 = Maps.newHashMap();

	private static final String CLASS1 = "class1";
	private static final String CLASS2 = "class2";
	private static final String PAGE1 = "page1";
	private static final String PAGE2 = "page2";	
	private static final String APP1 = "app1";
	private static final String APP2 = "app2";
	private static final String APP3 = "app3";
	private static final String CONAPP = "conApp";
	
	@Before
	public void setUp() {
		permset1 = new PermissionSet("permset1");
		seaPerms1.put(SetupEntityTypes.APEX_CLASS, Sets.newHashSet(CLASS1, CLASS2));
		seaPerms1.put(SetupEntityTypes.APEX_PAGE, Sets.newHashSet(PAGE1, PAGE2));
		seaPerms1.put(SetupEntityTypes.TABSET, Sets.newHashSet(APP1, APP2));
		seaMap1.put(ObjPermCategory.original, seaPerms1);
		permset1.setSeaPermMap(ObjPermCategory.original, seaPerms1);
		
		permset2 = new PermissionSet("permset2");
		seaPerms2.put(SetupEntityTypes.APEX_CLASS, Sets.newHashSet(CLASS1, CLASS2));
		seaPerms2.put(SetupEntityTypes.APEX_PAGE, Sets.newHashSet(PAGE1));
		seaPerms2.put(SetupEntityTypes.TABSET, Sets.newHashSet(APP3));
		seaMap2.put(ObjPermCategory.original, seaPerms2);
		permset2.setSeaPermMap(ObjPermCategory.original, seaPerms2);

		permset3 = new PermissionSet("permset3");
		seaPerms3.put(SetupEntityTypes.CONN_APP, Sets.newHashSet(CONAPP));
		seaMap3.put(ObjPermCategory.original, seaPerms3);
		permset3.setSeaPermMap(ObjPermCategory.original, seaPerms3);
		
		permset4 = new PermissionSet("permset4");
		seaMap4.put(ObjPermCategory.original, seaPerms4);
		permset4.setSeaPermMap(ObjPermCategory.original, seaPerms4);
		
	}
	
	@Test
	public void testOnePermset() {
		CompareSetupEntityPerms.classifySetupEntityPerms(new PermissionSet[] {permset1});
		
		Map<SetupEntityTypes, Set<String>> seaMap = permset1.getSeaPermMap(ObjPermCategory.common);
    	checkSeaMap(seaPerms1, seaMap);
    	
    	seaMap = permset1.getSeaPermMap(ObjPermCategory.unique);
    	checkSeaMap(seaPerms1, seaMap);
    	
    	seaMap = permset1.getSeaPermMap(ObjPermCategory.differing);
    	checkSeaMap(Maps.<SetupEntityTypes, Set<String>>newHashMap(), seaMap);
	}
	
	@Test
	public void testCompareSamePermset() {
		CompareSetupEntityPerms.classifySetupEntityPerms(new PermissionSet[] {permset1, permset1});
		
		Map<SetupEntityTypes, Set<String>> seaMap = permset1.getSeaPermMap(ObjPermCategory.common);
    	checkSeaMap(seaPerms1, seaMap);
    	
    	seaMap = permset1.getSeaPermMap(ObjPermCategory.unique);
    	checkSeaMap(Maps.<SetupEntityTypes, Set<String>>newHashMap(), seaMap);
    	
    	seaMap = permset1.getSeaPermMap(ObjPermCategory.differing);
    	checkSeaMap(Maps.<SetupEntityTypes, Set<String>>newHashMap(), seaMap);
	}
	
    @Test
    public void testNoCommonSeaCommonPerms() {
		CompareSetupEntityPerms.classifySetupEntityPerms(new PermissionSet[] {permset1, permset3});
		
		Map<SetupEntityTypes, Set<String>> seaMap1 = permset1.getSeaPermMap(ObjPermCategory.common);
		Map<SetupEntityTypes, Set<String>> seaMap3 = permset3.getSeaPermMap(ObjPermCategory.common);
    	checkSeaMap(Maps.<SetupEntityTypes, Set<String>>newHashMap(), seaMap1);
    	checkSeaMap(Maps.<SetupEntityTypes, Set<String>>newHashMap(), seaMap3);
    }
    
    @Test
    public void testCommonPerms() {
		CompareSetupEntityPerms.classifySetupEntityPerms(new PermissionSet[] {permset1, permset2});
		
		Map<SetupEntityTypes, Set<String>> seaMap1 = permset1.getSeaPermMap(ObjPermCategory.common);
		Map<SetupEntityTypes, Set<String>> seaMap2 = permset2.getSeaPermMap(ObjPermCategory.common);
		
		Map<SetupEntityTypes, Set<String>> expectedCommonMap = Maps.newHashMap();
		expectedCommonMap.put(SetupEntityTypes.APEX_CLASS, Sets.newHashSet(CLASS1, CLASS2));
		expectedCommonMap.put(SetupEntityTypes.APEX_PAGE, Sets.newHashSet(PAGE1));
		
    	checkSeaMap(expectedCommonMap, seaMap1);
    	checkSeaMap(expectedCommonMap, seaMap2);
    }
    
    @Test
    public void testNoCommonSeaUniquePerms() {
		CompareSetupEntityPerms.classifySetupEntityPerms(new PermissionSet[] {permset1, permset3});

		Map<SetupEntityTypes, Set<String>> seaMap1 = permset1.getSeaPermMap(ObjPermCategory.unique);
		Map<SetupEntityTypes, Set<String>> seaMap3 = permset3.getSeaPermMap(ObjPermCategory.unique);
		
    	checkSeaMap(seaPerms1, seaMap1);
    	checkSeaMap(seaPerms3, seaMap3);
    }
    
    @Test
    public void testUniquePerms() {
    	CompareSetupEntityPerms.classifySetupEntityPerms(new PermissionSet[] {permset1, permset2});
		
		Map<SetupEntityTypes, Set<String>> seaMap1 = permset1.getSeaPermMap(ObjPermCategory.unique);
		Map<SetupEntityTypes, Set<String>> seaMap2 = permset2.getSeaPermMap(ObjPermCategory.unique);

		Map<SetupEntityTypes, Set<String>> expectedUniqueMap1 = Maps.newHashMap();
		expectedUniqueMap1.put(SetupEntityTypes.APEX_PAGE, Sets.newHashSet(PAGE2));
		expectedUniqueMap1.put(SetupEntityTypes.TABSET, Sets.newHashSet(APP1, APP2));

		Map<SetupEntityTypes, Set<String>> expectedUniqueMap2 = Maps.newHashMap();
		expectedUniqueMap2.put(SetupEntityTypes.TABSET, Sets.newHashSet(APP3));
		
		checkSeaMap(expectedUniqueMap1, seaMap1);
    	checkSeaMap(expectedUniqueMap2, seaMap2);
    }
    
    @Test
    public void testNoCommonSeaDifferingPerms() {
		CompareSetupEntityPerms.classifySetupEntityPerms(new PermissionSet[] {permset1, permset3});

		Map<SetupEntityTypes, Set<String>> seaMap1 = permset1.getSeaPermMap(ObjPermCategory.differing);
		Map<SetupEntityTypes, Set<String>> seaMap3 = permset3.getSeaPermMap(ObjPermCategory.differing);

    	checkSeaMap(seaPerms1, seaMap1);
    	checkSeaMap(seaPerms3, seaMap3);
    }
    
    @Test
    public void testDifferingPerms() {
    	CompareSetupEntityPerms.classifySetupEntityPerms(new PermissionSet[] {permset1, permset2});
		
		Map<SetupEntityTypes, Set<String>> seaMap1 = permset1.getSeaPermMap(ObjPermCategory.differing);
		Map<SetupEntityTypes, Set<String>> seaMap2 = permset2.getSeaPermMap(ObjPermCategory.differing);

		Map<SetupEntityTypes, Set<String>> expectedUniqueMap1 = Maps.newHashMap();
		expectedUniqueMap1.put(SetupEntityTypes.APEX_PAGE, Sets.newHashSet(PAGE2));
		expectedUniqueMap1.put(SetupEntityTypes.TABSET, Sets.newHashSet(APP1, APP2));

		Map<SetupEntityTypes, Set<String>> expectedUniqueMap2 = Maps.newHashMap();
		expectedUniqueMap2.put(SetupEntityTypes.TABSET, Sets.newHashSet(APP3));
		
		checkSeaMap(expectedUniqueMap1, seaMap1);
    	checkSeaMap(expectedUniqueMap2, seaMap2);
    }
    
    @Test
    public void testDifferingPermsetsOneEmpty() {
    	CompareSetupEntityPerms.classifySetupEntityPerms(new PermissionSet[] {permset1, permset2, permset3, permset4});
		
		Map<SetupEntityTypes, Set<String>> seaMap1 = permset1.getSeaPermMap(ObjPermCategory.differing);
		Map<SetupEntityTypes, Set<String>> seaMap2 = permset2.getSeaPermMap(ObjPermCategory.differing);
		Map<SetupEntityTypes, Set<String>> seaMap3 = permset3.getSeaPermMap(ObjPermCategory.differing);
		Map<SetupEntityTypes, Set<String>> seaMap4 = permset4.getSeaPermMap(ObjPermCategory.differing);

		checkSeaMap(seaPerms1, seaMap1);
    	checkSeaMap(seaPerms2, seaMap2);
    	checkSeaMap(seaPerms3, seaMap3);
    	checkSeaMap(seaPerms4, seaMap4);
    }
    
	private void checkSeaMap(Map<SetupEntityTypes, Set<String>> expectedSeaMap, Map<SetupEntityTypes, Set<String>> actualSeaMap) {
		Set<SetupEntityTypes> expectedKeys = expectedSeaMap.keySet();
		Set<SetupEntityTypes> actualKeys = actualSeaMap.keySet();
		
		assertEquals(String.format("SeaPermMap should contain %s key(s) but contained %s", expectedKeys.size(), actualKeys.size()), 
				expectedKeys.size(), actualKeys.size());
    	for (SetupEntityTypes key : expectedKeys) {
    		assertTrue("SeaPermMap should have contained key " + key, actualKeys.contains(key));
    		
			assertEquals(
					"Set of Ids did not match for SetupEntityType: " + key,
					expectedSeaMap.get(key), actualSeaMap.get(key));
    	}
	}
}
