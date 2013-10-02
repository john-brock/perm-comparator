package controllers.CompareUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.map.HashedMap;

import play.Logger;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import controllers.RetrieveData;

import models.PermissionSet;
import models.PermissionSet.ObjPermCategory;
import models.PermissionSet.objectPermissions;

public class CompareObjectPerms extends BaseCompare {

	/**
	 * Fills a permset's ObjectPermMap
	 * @param parentId - UserId or PermissionSet / ProfileId
	 */
	protected static void addPermsToPermset(PermissionSet permset, boolean retry) {
		permset.setObjPermMap(ObjPermCategory.original,
				CompareObjectPerms.getObjectPermsMap(permset.getId(), retry));
	}
	
	/**
	 * Returns object perm SOQL query string
	 * @param parentId - UserId or PermissionSet / ProfileId
	 */
	private static String buildObjPermQuery(String parentId) {
		StringBuilder query = new StringBuilder("SELECT SobjectType,");
		appendParamsToQuery(query, OBJECT_PERMS, null);
		query.append(" FROM ObjectPermissions WHERE ParentId");
		if (parentId.startsWith(PERMSET_ID_PREFIX)) {
			query.append("='").append(parentId).append("'");
		} else {
			query.append(" IN (SELECT PermissionSetId from PermissionSetAssignment WHERE ");
			if (parentId.startsWith(USER_ID_PREFIX)) {
				query.append("AssigneeId='");
			} else if (parentId.startsWith(PROFILE_ID_PREFIX)) {
				query.append("ProfileId='");
			} else {
				Logger.warn("Invalid parentId prefix.  ParentId: %s", parentId);
			}
			// TODO throw exception - don't allow invalid query string
			query.append(parentId).append("')");
		}
		query.append(" ORDER BY SobjectType ASC NULLS FIRST");
		return query.toString();
	}
	
	/**
	 * Query for object perms associated with given userId and place data in Map
	 * @param parentId
	 */
	protected static Map<String, EnumSet<PermissionSet.objectPermissions>> getObjectPermsMap(
			String parentId, boolean retry) {
		String query = buildObjPermQuery(parentId);
		Map<String, EnumSet<PermissionSet.objectPermissions>> objPermMap = Maps.newHashMap();
		Logger.info("QUERY: " + query);
		JsonObject objPermResults = RetrieveData.query(query, retry);
		JsonArray objPermRows = null;
		if (objPermResults != null) {
			objPermRows = objPermResults.get("records").getAsJsonArray();
		} else {
			Logger.warn("ObjPermResults was null. Query: %s", query);
		}
		// add object perm data to map
		for (JsonElement objRow : objPermRows) {
			JsonObject row = objRow.getAsJsonObject();
			String objName = row.get("SobjectType").getAsString();
			EnumSet<PermissionSet.objectPermissions> objPerms = EnumSet.noneOf(PermissionSet.objectPermissions.class);
			
			// for each object perm, add to set if value is true
			for (PermissionSet.objectPermissions objPerm : PermissionSet.objectPermissions.values()) {
				if (row.get(objPerm.toString()).getAsBoolean()) {
					objPerms.add(objPerm);
				}
			}
			// if multiple rows for same object, merge sets for aggregate
			if (objPermMap.containsKey(objName)) {
				objPerms.addAll(objPermMap.get(objName));
			}
			objPermMap.put(objName, objPerms);
		}
		Logger.info("OBJ_PERM_MAP: %s", objPermMap);
		return objPermMap;
	}
	
	public static String compareObjPerms(boolean retry, String... ids) {
		PermissionSet[] permsets = getPermsetArray(retry, OBJECT_PERMS, ids);
		classifyObjectPerms(permsets);

		return generatePermsJson(permsets, OBJECT_PERMS);
	}

	/**
	 * Performs comparison operations on effective object perms to classify Unique, Common, and Difference perms.
	 * Sets respective EnumSet on the permission set object
	 * @param PermissionSet[] permsets
	 */
	public static void classifyObjectPerms(PermissionSet[] permsets) {
		for (int i=0; i<permsets.length; i++) {
			if (permsets[i] != null) {
				findAndSetUniqueObjPerms(permsets, i);
				findAndSetCommonObjPerms(permsets, i);
				findAndSetDifferingObjPerms(permsets, i);
			}
		}
	}
	
	/**
	 * Find the unique object perms and set for each permset
	 */
	private static void findAndSetUniqueObjPerms(PermissionSet[] permsets, int currentIndex) {
		// start with copy of original perms and remove all non-unique perms
		Map<String, EnumSet<PermissionSet.objectPermissions>> uniqueObjectPermMap = new HashMap();
		copyOriginalObjectMapIntoMap(uniqueObjectPermMap, permsets, currentIndex);
		Set<String> objectKeysToRemove = new HashSet();
		
		for (int j = 0; j < permsets.length; j++) {
			// ensure not comparing permset to itself (but, will compare if placed in toolbar twice)
			if ((permsets[j] != null) && (j != currentIndex)) {
				// unique perms = set of perms minus intersection with each other permset
				if ((uniqueObjectPermMap != null) && (permsets[j].getOjPermMap(ObjPermCategory.original) != null)) {
					for (String objectName : uniqueObjectPermMap.keySet()) {
						if (permsets[j].getOjPermMap(ObjPermCategory.original).get(objectName) != null) {
							uniqueObjectPermMap.get(objectName)
									.removeAll(permsets[j].getOjPermMap(ObjPermCategory.original).get(objectName));
							if (uniqueObjectPermMap.get(objectName).isEmpty()) { objectKeysToRemove.add(objectName); }
						}
					}
				}
			}
		}
		for (String objectKeyToRemove : objectKeysToRemove) {
			uniqueObjectPermMap.remove(objectKeyToRemove);
		}
		permsets[currentIndex].setObjPermMap(PermissionSet.ObjPermCategory.unique, uniqueObjectPermMap);
	}
	
	/**
	 * Find the common object perms and set for each permset
	 */
	private static void findAndSetCommonObjPerms(PermissionSet[] permsets, int currentIndex) {
		Map<String, EnumSet<PermissionSet.objectPermissions>> commonObjectPermMap = new HashMap();
		Set<String> objKeysToRemove = new HashSet();

		copyOriginalObjectMapIntoMap(commonObjectPermMap, permsets, currentIndex);
		
		// common perms finds the intersection with each permset
		for (int j = 0; j < permsets.length; j++) {
			// ensure not comparing to permset to itself (but, will compare if placed in toolbar twice)
			if ((permsets[j] != null) && (j != currentIndex)) {
				if ((commonObjectPermMap != null) && (permsets[j].getOjPermMap(ObjPermCategory.original) != null)) {
					if (commonObjectPermMap.keySet().size() > 0) {
						Iterator objects = commonObjectPermMap.keySet().iterator();
						while (objects.hasNext()) {
							String object = objects.next().toString();
							if ((permsets[j].getOjPermMap(ObjPermCategory.original).containsKey(object))) {
								// if size is 0 for object --> no common perms, thus retain none
								commonObjectPermMap.get(object).retainAll(permsets[j].getOjPermMap(ObjPermCategory.original)
										.get(object));
								// mark object key to remove from commonPermMap if no permissions for obj --> no common
								if (commonObjectPermMap.get(object).isEmpty()) { objKeysToRemove.add(object); }
							}
							else {
								commonObjectPermMap.get(object).removeAll(Arrays.asList(objectPermissions.values()));
								objKeysToRemove.add(object);
							}
						}
					}
				}
			}
		}
		
		for (String objKeyToRemove : objKeysToRemove) {
			commonObjectPermMap.remove(objKeyToRemove);
		}
		permsets[currentIndex].setObjPermMap(PermissionSet.ObjPermCategory.common, commonObjectPermMap);
	}
	
	/**
	 * Find the differing object perms (original - common) and set for each permset
	 */
	private static void findAndSetDifferingObjPerms(PermissionSet[] permsets, int currentIndex) {
		// set differing = original and remove common
		Map<String, EnumSet<PermissionSet.objectPermissions>> differingObjPermMap = new HashMap();
		copyOriginalObjectMapIntoMap(differingObjPermMap, permsets, currentIndex);

		Map<String, EnumSet<PermissionSet.objectPermissions>> commonObjPermMap = new HashMap();
		copyOriginalObjectMapIntoMap(commonObjPermMap, ObjPermCategory.common, permsets, currentIndex);

		Set<String> objKeysToRemove = new HashSet();
		
		// difference perms is simply all original perms minus intersection with common perms
		for (String object : commonObjPermMap.keySet()) {
			if (!differingObjPermMap.containsKey(object)) {
				objKeysToRemove.add(object);
			} else {
				differingObjPermMap.get(object).removeAll(commonObjPermMap.get(object));
				if (differingObjPermMap.get(object).isEmpty()) { objKeysToRemove.add(object); }
			}
		}
		for (String objKeyToRemove : objKeysToRemove) {
			differingObjPermMap.remove(objKeyToRemove);
		}
		
		permsets[currentIndex].setObjPermMap(PermissionSet.ObjPermCategory.differing, differingObjPermMap);
	}
	
	/**
	 * Add unique, common, and difference object perms for permset to StringBuilder
	 * @param PermissionSet - permset
	 * @param StringBuilder - jsonBuild
	 * @param String - permCategory
	 * @param int - current permset number
	 */
	public static void addObjPermResultsToJson(PermissionSet permset,
			StringBuilder jsonBuild, String permCategory, int i) {
		
		Iterator objPermItter = null;
		Iterator objItter = null;

		String objName = null;
		int endOfPermissionsIndex = 11;
		Map<String, EnumSet<objectPermissions>> objPermMap = new HashMap();

		StringBuilder permsetRoot = new StringBuilder();
		permsetRoot.append("permset").append(i+1).append(OBJECT);
		if (permCategory.equals(UNIQUE)) {
			objPermMap = permset.getOjPermMap(ObjPermCategory.unique);
			permsetRoot.append(UNIQUE);
			
		} else if (permCategory.equals(COMMON)) {
			objPermMap = permset.getOjPermMap(ObjPermCategory.common);
			permsetRoot.append(COMMON);
			
		} else if (permCategory.equals(DIFFERENCES)) {
			objPermMap = permset.getOjPermMap(ObjPermCategory.differing);
			permsetRoot.append(DIFFERENCES);
		}
		
		String permsetLabel = permsetRoot.toString();
		jsonBuild.append(", ").append("\"" + permsetLabel + "\"").append(": [");
		
		SortedSet<String> alphabeticalKeySet = new TreeSet<String>();
		for (String objNameKey : objPermMap.keySet()) {
			alphabeticalKeySet.add(objNameKey);
		}
		objItter = alphabeticalKeySet.iterator();
		if (objItter.hasNext()) {jsonBuild.append("{ \"success\": \"true\", \"text\": \"Objects\", \"").append(permsetLabel).append("\": [ "); }
		while (objItter.hasNext()) {
			objName = objItter.next().toString();
			jsonBuild.append("{\"success\": \"true\", \"text\": \"").append(objName).append("\", \"").append(permsetLabel).append("\": [");

			objPermItter = objPermMap.get(objName).iterator();
			while (objPermItter.hasNext()) {
				jsonBuild.append("{\"success\": \"true\", \"text\": \"");
				jsonBuild.append(objPermItter.next().toString().substring(endOfPermissionsIndex));
				jsonBuild.append("\", ").append("\"leaf\":\"true\", \"icon\":\"../../resources/themes/images/default/tree/checkMark.png\", \"loaded\":\"true\" }");			// currently not used, but just for demo
				if (objPermItter.hasNext()) { jsonBuild.append(", "); }	// if next, comma, otherwise, no comma
			} 
			jsonBuild.append("], \"leaf\":\"false\", \"expanded\":\"true\", \"loaded\":\"true\"}");
			if (objItter.hasNext()) { 
				jsonBuild.append(", "); 
			} else {
				jsonBuild.append(" ], \"leaf\":\"false\", \"expanded\":\"true\", \"loaded\":\"true\"}");
			}
		}
		jsonBuild.append("]");
	}
	
	/**
	 * Utility method to copy the original object perm map
	 */
	protected static void copyOriginalObjectMapIntoMap(Map<String, EnumSet<objectPermissions>> map, PermissionSet[] permsets, int currentIndex) {
		copyOriginalObjectMapIntoMap(map, ObjPermCategory.original, permsets, currentIndex);
	}

	/**
	 * Utility method to copy the map of object permissions
	 */
	private static void copyOriginalObjectMapIntoMap(Map<String, EnumSet<objectPermissions>> map, ObjPermCategory category, PermissionSet[] permsets, int currentIndex) {
		Map<String, EnumSet<PermissionSet.objectPermissions>> originalMap = permsets[currentIndex].getOjPermMap(category);
		for (String key : originalMap.keySet()) {
			map.put(key, originalMap.get(key).clone());
		}
	}
}
