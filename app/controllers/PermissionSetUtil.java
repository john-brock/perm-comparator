package controllers;

import java.lang.reflect.Array;
import java.util.ArrayList;
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

import models.PermissionSet;
import models.PermissionSet.ObjPermCategory;
import models.PermissionSet.objectPermissions;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;

public class PermissionSetUtil {
	
	private static final String USER = "_User";
	private static final String OBJECT = "_Object";

	private static final String UNIQUE = "_Unique";
	private static final String COMMON = "_Common";
	private static final String DIFFERENCES = "_Differences";
	
	private static final String USER_ID_PREFIX = "005";
	private static final String PROFILE_ID_PREFIX = "00e";
	private static final String PERMSET_ID_PREFIX = "0PS";
	
	private static final String USER_PERMS = "UserPerms";
	private static final String OBJECT_PERMS = "ObjectPerms";
	
	/**
	 * Build SOQL query string for all permissions
	 * @param permsetId
	 */
	protected static String queryBuilder(String permsetId, Set<String> userPerms) {
		StringBuilder queryBuild = new StringBuilder();
		queryBuild.append("SELECT ");

		appendParamsToQuery(queryBuild, USER_PERMS, userPerms);
		queryBuild.append(" FROM PermissionSet WHERE Id=\'").append(permsetId).append("\'");

		return queryBuild.toString();
	}

	/**
	 * Append permissions to query from PermissionSet
	 * @param queryBuild - StringBuilder
	 * @param permCategory - User or Object permissions
	 */
	private static void appendParamsToQuery(StringBuilder queryBuild, String permCategory, Set<String> userPerms) {
		String[] perms = new String[]{};
		if (permCategory.equals(USER_PERMS)) {
			if (userPerms != null) {
				perms = Arrays.copyOf(userPerms.toArray(), userPerms.size(), String[].class);
			}
		} 
		else if (permCategory.equals(OBJECT_PERMS)) {
			PermissionSet.objectPermissions[] enumValues = PermissionSet.objectPermissions.values();
			int enumLength = enumValues.length;
			String[] objPerms = new String[enumLength];
			for (int i=0; i < enumLength; i++) {
				objPerms[i] = enumValues[i].toString();
			}
			perms = objPerms;
		}
		
		int numOfPerms = perms.length;
		for (int i=0; i < numOfPerms; i++) {
			queryBuild.append(perms[i].toString());
			if (i < numOfPerms-1)
				queryBuild.append(", ");
		}
	}

	private static Set<String> retrieveValidUserPerms() {
		JsonObject permsetData = RetrieveData.getUserPerms(true);
		JsonArray permsetFields = null;
		if (permsetData != null) {
			permsetFields = permsetData.get("fields").getAsJsonArray();
		} else {
			Logger.error("No data retrieved when fetching user perms");
		}
		
		if (permsetFields != null) {
			int validUserPermsLength = permsetFields.size();
			Set<String> userPerms = new HashSet<String>();
			for (int i=0; i < validUserPermsLength; i++) {
				String permName = permsetFields.get(i).getAsJsonObject().get("name").getAsString();
				if (permName.startsWith("Permissions")) {
					userPerms.add(permName);
				}
			}
			return userPerms;
		} else {
			Logger.warn("PermsetFields was null");
			return null;
		}
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
	 * Creates new permission set object to hold perms for comparisons.
	 * Fills user perms before returning object based on permsetId
	 * @param permsetId
	 * @param retry
	 */
	public static PermissionSet getPermissionSet(String permsetId, boolean retry) {
		PermissionSet permset = new PermissionSet(permsetId);

		Set<String> userPerms = retrieveValidUserPerms();
		String query = queryBuilder(permsetId, userPerms);
		JsonObject permsetInfo = RetrieveData.query(query, retry).get("records").getAsJsonArray().get(0).getAsJsonObject();

		if (permsetInfo == null) {
			Logger.warn("PermsetInfo is null after query in getPermissionSet. Query: %s", query);
		}
		addUserPermsToPermset(permset, userPerms, permsetInfo);
		return permset;
	}
	
	/**
	 * Add permissions that are set to true to the PermissionSet
	 * @param permset
	 * @param permsetInfo
	 */
	private static void addUserPermsToPermset(PermissionSet permset, Set<String> userPerms, JsonObject permsetInfo) {
		for (String perm : userPerms) {
			if (permsetInfo.has(perm)) {
				// if permission is allowed (true) - add to enumSet
				if (permsetInfo.get(perm).getAsBoolean() == true) {
					permset.getUserPerms().add(perm);
				}
			}
		}
	}
	
	/**
	 * Query for object perms associated with given userId and place data in Map
	 * @param userId
	 */
	public static Map<String, EnumSet<PermissionSet.objectPermissions>> getObjectPermsMap(
			String userId, boolean retry) {
		String query = buildObjPermQuery(userId);
		Map<String, EnumSet<PermissionSet.objectPermissions>> objPermMap = new HashedMap();
//		Logger.info("QUERY: " + query);
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
			// if multiple rows for same object, merge sets to get aggregate set
			if (objPermMap.containsKey(objName)) {
				objPerms.addAll(objPermMap.get(objName));
			}
			objPermMap.put(objName, objPerms);
		}
		return objPermMap;
	}
	
	/**
	 * Gets ImmutableSet<String> of permset ids assigned to specified user
	 * @param userId
	 * @param retry
	 * @return ImmutableSet - permset ids
	 */
	public static ImmutableSet<String> getUserPermsetIds(String userId, boolean retry) {
		ImmutableSet.Builder<String> idsBuilder = new ImmutableSet.Builder<String>();
		
		// query returns permission sets and profile permission set for the user
		String query = "SELECT PermissionSet.Id FROM PermissionSetAssignment WHERE AssigneeId='" + userId + "'";
		JsonArray permsetInfo = RetrieveData.query(query, retry).get("records").getAsJsonArray();
		if (permsetInfo.size() == 0) {
			Logger.warn("No permsets assigned to user found - should have >= 1");
		}
		
		for (JsonElement returnedPermset : permsetInfo) {
			idsBuilder.add(returnedPermset.getAsJsonObject().get("PermissionSet").getAsJsonObject().get("Id").getAsString());
		}

		return idsBuilder.build();
	}
	
	public static String comparePermsets(boolean retry, String... ids) {
		PermissionSet[] permsets = getPermsetArray(retry, ids);
		classifyUserPerms(permsets);

		return generatePermsJson(permsets, "userPerms");
	}

	public static String compareObjPerms(boolean retry, String... ids) {
		PermissionSet[] permsets = getPermsetArray(retry, ids);
		classifyObjectPerms(permsets);

		return generatePermsJson(permsets, "objectPerms");
	}
	
	/**
	 * Fill PermissionSet array with permset objects 
	 * @param ids - ids (can be user, permission set, or profile)
	 * @return PermissionSet[]
	 */
	private static PermissionSet[] getPermsetArray(boolean retry, String... ids) {
		int numberOfIds = ids.length;
		PermissionSet[] permsets = new PermissionSet[numberOfIds];
		
		for (int i=0; i < numberOfIds; i++) {
			String id = ids[i];
			if (id.contains("blank"))
				permsets[i] = null;
			else {
				if (id.startsWith(USER_ID_PREFIX)) {
					permsets[i] = getEffectiveUserPermset(id);
				}
				// TODO add another else to prevent invalid spoofs of url - ensure prefix valid
				else {
					permsets[i] = getPermissionSet(id, retry);
				}
				permsets[i].setObjPermMap(PermissionSet.ObjPermCategory.original,
						getObjectPermsMap(id, retry));
			}
		}
		return permsets;
	}
	
	/**
	 * Creates aggregate permset representing effective perms of a user
	 * @param ImmutableSet<PermissionSet> permsets
	 * @return PermissionSet 
	 */
	public static PermissionSet aggregatePermissionSets(ImmutableSet<PermissionSet> permsets) {
		PermissionSet permset = new PermissionSet("aggregatePermset_FakeId");
		Set<String> aggregatePerms = new HashSet<String>();
		Iterator permIter = permsets.iterator();
		while (permIter.hasNext()) {
			aggregatePerms.addAll(((PermissionSet) permIter.next()).getUserPerms());
		}
		permset.setUserPerms(aggregatePerms);
		return permset;
	}
	
	/**
	 * Get permission set with effective user perms with userId
	 * @param userId
	 * @return PermissionSet
	 */
	private static PermissionSet getEffectiveUserPermset(String userId) {
		ImmutableSet.Builder<PermissionSet> permsetSetBuilder = new ImmutableSet.Builder<PermissionSet>();
		boolean retry = true;
		
		ImmutableSet<String> permsetIds = getUserPermsetIds(userId, retry);
		Iterator ittr = permsetIds.iterator();
		while (ittr.hasNext())
			permsetSetBuilder.add(getPermissionSet(ittr.next().toString(), retry));
		
		return aggregatePermissionSets(permsetSetBuilder.build());
	}
	
	/**
	 * Performs comparison operations on effective perms to find Unique, Common, and Differences.
	 * Sets respective EnumSet on the permission set object
	 * @param PermissionSet[] permsets
	 */
	public static void classifyUserPerms(PermissionSet[] permsets) {
		int numberOfPermsets = permsets.length;

		for (int i=0; i<numberOfPermsets; i++) {
			if (permsets[i] != null) {
				// start with copy of user perms
				Set<String> uniquePerms = new HashSet<String>(permsets[i].getUserPerms());
				Set<String> commonPerms = new HashSet<String>(permsets[i].getUserPerms());

				for (int j=0; j<numberOfPermsets; j++) {
					// ensure not comparing to permset to itself (will compare if placed in toolbar twice)
					if ((permsets[j] != null) && (j != i)) {
						// unique perms = set of perms minus intersection with each other permset
						uniquePerms.removeAll(permsets[j].getUserPerms());

						// common perms finds the intersection with each permset (this could actually run only one time)
						commonPerms.retainAll(permsets[j].getUserPerms());
					}
				}
				permsets[i].setUniqueUserPerms(uniquePerms);
				permsets[i].setCommonUserPerms(commonPerms);
				
				Set<String> differencePerms = new HashSet<String>(permsets[i].getUserPerms());

				// difference perms is simply perms minus intersection with all others (common perms)
				differencePerms.removeAll(commonPerms);
				permsets[i].setDifferenceUserPerms(differencePerms);
			}
		}
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
	 * Utility method to copy the original object perm map
	 */
	private static void copyOriginalObjectMapIntoMap(Map<String, EnumSet<objectPermissions>> map, PermissionSet[] permsets, int currentIndex) {
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
	 * Returns Json-like String representation of comparison results
	 * @param PermissionSet[] permsets
	 */
	private static String generatePermsJson(PermissionSet[] permsets, String permType) {
		int numberOfPermsets = permsets.length;
		
		StringBuilder jsonBuild = new StringBuilder();
		jsonBuild.append("{\'numberOfPermsets\': ").append(numberOfPermsets);
		
		for (int i=0; i<numberOfPermsets; i++) {
			if (permsets[i] != null) {
				if (permType.contains("userPerms")) {
					addUserPermResultsToJson(permsets[i], jsonBuild, UNIQUE, i);
					addUserPermResultsToJson(permsets[i], jsonBuild, COMMON, i);
					addUserPermResultsToJson(permsets[i], jsonBuild, DIFFERENCES, i);
					
				} else if (permType.contains("objectPerms")) {
					addObjPermResultsToJson(permsets[i], jsonBuild, UNIQUE, i);
					addObjPermResultsToJson(permsets[i], jsonBuild, COMMON, i);
					addObjPermResultsToJson(permsets[i], jsonBuild, DIFFERENCES, i);
				}
			}
		}
		jsonBuild.append(" }");
//		Logger.info("JSON RESULT: " + jsonBuild.toString());
		return jsonBuild.toString();
	}

	/**
	 * Add unique, common, and difference user perms for permset to StringBuilder
	 * @param PermissionSet - permset
	 * @param StringBuilder - jsonBuild
	 * @param String - permCategory
	 * @param int - current permset number
	 */
	private static void addUserPermResultsToJson(PermissionSet permset,
			StringBuilder jsonBuild, String permCategory, int i) {
		jsonBuild.append(", permset").append(i+1);
		
		Iterator itter = null;
		jsonBuild.append(USER);
		
		Set<String> userPerms = new HashSet<String>();
		if (permCategory.equals(UNIQUE)) {
			userPerms = permset.getUniqueUserPerms();
			jsonBuild.append(UNIQUE);
			
		} else if (permCategory.equals(COMMON)) {
			userPerms = permset.getCommonUserPerms();
			jsonBuild.append(COMMON);
			
		} else if (permCategory.equals(DIFFERENCES)) {
			userPerms = permset.getDifferenceUserPerms();
			jsonBuild.append(DIFFERENCES);
		}
		
		jsonBuild.append(": [");

		Set<String> sortedPerms = new TreeSet<String>(userPerms);
		itter = sortedPerms.iterator();

		int endOfPermissionsIndex = 11;
		// add each perm to the json result
		while (itter.hasNext()) {
			jsonBuild.append("{\'name': \'");
			
			// 'Permissions' is 11 chars - remove from front and split on Uppercase, not first
			String[] permLableSubstrings = itter.next().toString()
					.substring(endOfPermissionsIndex)
					.split("(?<!^)(?=\\p{Upper})");
			
			for (String substring : permLableSubstrings) {
				jsonBuild.append(substring).append(" ");	// splits strings to array, add back
			}
			jsonBuild.append("\', 'enabled': true }");		// currently not used, but just for demo
			if (itter.hasNext()) jsonBuild.append(", ");	// if next, comma, otherwise, no comma
		}
		jsonBuild.append("]");
	}
	
	/**
	 * Add unique, common, and difference object perms for permset to StringBuilder
	 * @param PermissionSet - permset
	 * @param StringBuilder - jsonBuild
	 * @param String - permCategory
	 * @param int - current permset number
	 */
	private static void addObjPermResultsToJson(PermissionSet permset,
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
		jsonBuild.append(", ").append(permsetLabel).append(": [");
		
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
}
