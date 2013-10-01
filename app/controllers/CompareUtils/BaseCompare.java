package controllers.CompareUtils;

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
import models.PermissionSet.SetupEntityTypes;
import models.PermissionSet.objectPermissions;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;

import controllers.RetrieveData;

public class BaseCompare {
	
	protected static final String USER = "_User";
	protected static final String OBJECT = "_Object";

	protected static final String UNIQUE = "_Unique";
	protected static final String COMMON = "_Common";
	protected static final String DIFFERENCES = "_Differences";
	
	protected static final String USER_ID_PREFIX = "005";
	protected static final String PROFILE_ID_PREFIX = "00e";
	protected static final String PERMSET_ID_PREFIX = "0PS";
	
	protected static final String USER_PERMS = "UserPerms";
	protected static final String OBJECT_PERMS = "ObjectPerms";
	protected static final String SETUP_ENTITY_PERMS = "SetupEntityPerms";
	
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
	protected static void appendParamsToQuery(StringBuilder queryBuild, String permCategory, Set<String> userPerms) {
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

	/**
	 * Creates new permission set object to hold perms for comparisons.
	 * Fills user perms before returning object based on permsetId
	 * @param permsetId
	 * @param retry
	 */
	public static PermissionSet getPermissionSet(String permsetId, boolean retry) {
		PermissionSet permset = new PermissionSet(permsetId);

		CompareUserPerms.addPermsToPermset(permset, retry);
		CompareObjectPerms.addPermsToPermset(permset, retry);
		CompareSetupEntityPerms.addPermsToPermset(permset, retry);
		
		return permset;
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
			
	/**
	 * Fill PermissionSet array with permset objects 
	 * @param ids - ids (can be user, permission set, or profile)
	 * @return PermissionSet[]
	 */
	protected static PermissionSet[] getPermsetArray(boolean retry, String... ids) {
		int numberOfIds = ids.length;
		PermissionSet[] permsets = new PermissionSet[numberOfIds];
		
		for (int i=0; i < numberOfIds; i++) {
			String id = ids[i];
			if (id.contains("blank")) {
				permsets[i] = null;
			} else {
				if (id.startsWith(USER_ID_PREFIX)) {
					permsets[i] = getEffectiveUserPermset(id);
				}
				// TODO add another else to prevent invalid spoofs of url - ensure prefix valid
				else {
					permsets[i] = getPermissionSet(id, retry);
				}
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
		Set<String> aggregateUserPerms = new HashSet<String>();

		Iterator permIter = permsets.iterator();
		while (permIter.hasNext()) {
			PermissionSet tempPermset = (PermissionSet) permIter.next();
			aggregateUserPerms.addAll(tempPermset.getUserPerms());
			for (SetupEntityTypes type : SetupEntityTypes.values()) {
				permset.getSeaPermMap(ObjPermCategory.original)
						.get(type)
						.addAll(tempPermset.getSeaPermMap(
								ObjPermCategory.original).get(type));
			}
		}
		permset.setUserPerms(aggregateUserPerms);

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
	 * Returns Json-like String representation of comparison results
	 * @param PermissionSet[] permsets
	 */
	protected static String generatePermsJson(PermissionSet[] permsets, String permType) {
		int numberOfPermsets = permsets.length;
		
		StringBuilder jsonBuild = new StringBuilder();
		jsonBuild.append("{\"numberOfPermsets\": ").append(numberOfPermsets);
		
		for (int i=0; i<numberOfPermsets; i++) {
			if (permsets[i] != null) {
				if (permType.contains(USER_PERMS)) {
					CompareUserPerms.addUserPermResultsToJson(permsets[i], jsonBuild, UNIQUE, i);
					CompareUserPerms.addUserPermResultsToJson(permsets[i], jsonBuild, COMMON, i);
					CompareUserPerms.addUserPermResultsToJson(permsets[i], jsonBuild, DIFFERENCES, i);
					
				} else if (permType.contains(OBJECT_PERMS)) {
					CompareObjectPerms.addObjPermResultsToJson(permsets[i], jsonBuild, UNIQUE, i);
					CompareObjectPerms.addObjPermResultsToJson(permsets[i], jsonBuild, COMMON, i);
					CompareObjectPerms.addObjPermResultsToJson(permsets[i], jsonBuild, DIFFERENCES, i);
					
				} else if (permType.contains(SETUP_ENTITY_PERMS)) {
					CompareSetupEntityPerms.addSEAPermResultsToJson(permsets[i], jsonBuild, UNIQUE, i);
					CompareSetupEntityPerms.addSEAPermResultsToJson(permsets[i], jsonBuild, COMMON, i);
					CompareSetupEntityPerms.addSEAPermResultsToJson(permsets[i], jsonBuild, DIFFERENCES, i);
				}
			}
		}
		jsonBuild.append(" }");
		Logger.info("JSON RESULT: " + jsonBuild.toString());
		return jsonBuild.toString();
	}

}
