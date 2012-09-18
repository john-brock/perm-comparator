package controllers;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

import play.Logger;

import models.PermissionSet;
import models.PermissionSet.validUserPerms;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;

public class PermissionSetUtil {

	private static final String UNIQUE = "_Unique";
	private static final String COMMON = "_Common";
	private static final String DIFFERENCES = "_Differences";
	private static final String USER_ID_PREFIX = "005E";

	
	/**
	 * Build SOQL query string for all permissions
	 * @param permsetId
	 */
	protected static String queryBuilder(String permsetId) {
		StringBuilder queryBuild = new StringBuilder();
		queryBuild.append("SELECT ");

		PermissionSet.validUserPerms[] userPerms = PermissionSet.validUserPerms.values();
		int numOfPerms = userPerms.length;
		for (int i=0; i < numOfPerms; i++) {
			queryBuild.append(userPerms[i].toString());
			if (i < numOfPerms-1)
				queryBuild.append(", ");
		}
		queryBuild.append(" FROM PermissionSet WHERE Id=\'").append(permsetId).append("\'");

		return queryBuild.toString();
	}
	
	/**
	 * Creates new permission set object to hold perms for comparisons.
	 * Fills user perms before returning object based on permsetId
	 * @param permsetId
	 * @param retry
	 */
	public static PermissionSet getPermissionSet(String permsetId, boolean retry) {
		PermissionSet permset = new PermissionSet(permsetId);

		String query = queryBuilder(permsetId);
		JsonObject permsetInfo = RetrieveData.query(query, retry).get("records").getAsJsonArray().get(0).getAsJsonObject();

		// get permissions available to set in PermissionSet enum
		for (validUserPerms perm : PermissionSet.validUserPerms.values()) {
			String permName = perm.toString();
			if (permsetInfo.has(permName)) {
				// if permission is allowed (true) - add to enumSet
				if (permsetInfo.get(permName).getAsBoolean() == true)
					permset.getUserPerms().add(perm);
			}
		}
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
		for (JsonElement returnedPermset : permsetInfo) {
			idsBuilder.add(returnedPermset.getAsJsonObject().get("PermissionSet").getAsJsonObject().get("Id").getAsString());
		}

		return idsBuilder.build();
	}
	
	public static String comparePermsets(boolean retry, String... ids) {
		PermissionSet[] permsets = getPermsetArray(retry, ids);
		findUniqueCommonAndDifferencePerms(permsets);
		
		return generatePermsJson(permsets);
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
			if (ids[i].contains("blank"))
				permsets[i] = null;
			else {
				if (ids[i].startsWith(USER_ID_PREFIX)) {
					permsets[i] = getEffectiveUserPermset(ids[i]);
				}
				else
					permsets[i] = getPermissionSet(ids[i], retry);
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
		EnumSet<PermissionSet.validUserPerms> aggregatePerms = EnumSet.noneOf(PermissionSet.validUserPerms.class);

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
	private static void findUniqueCommonAndDifferencePerms(PermissionSet[] permsets) {
		int numberOfPermsets = permsets.length;

		for (int i=0; i<numberOfPermsets; i++) {
			if (permsets[i] != null) {
				// start with copy of user perms
				EnumSet<PermissionSet.validUserPerms> uniquePerms = EnumSet.copyOf(permsets[i].getUserPerms());
				EnumSet<PermissionSet.validUserPerms> commonPerms = EnumSet.copyOf(permsets[i].getUserPerms());
				
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

				EnumSet<PermissionSet.validUserPerms> differencePerms = EnumSet.copyOf(permsets[i].getUserPerms());
				// difference perms is simply perms minus intersection with all others (common perms)
				differencePerms.removeAll(commonPerms);
				
				permsets[i].setDifferenceUserPerms(differencePerms);
			}
		}
	}
	
	/**
	 * Returns Json-like String representation of comparison results
	 * @param PermissionSet[] permsets
	 */
	private static String generatePermsJson(PermissionSet[] permsets) {
		int numberOfPermsets = permsets.length;
		
		StringBuilder jsonBuild = new StringBuilder();
		jsonBuild.append("{\'numberOfPermsets\': ").append(numberOfPermsets);
		
		for (int i=0; i<numberOfPermsets; i++) {
			if (permsets[i] != null) {
				addCompareResultsToJson(permsets[i], jsonBuild, UNIQUE, i);
				addCompareResultsToJson(permsets[i], jsonBuild, COMMON, i);
				addCompareResultsToJson(permsets[i], jsonBuild, DIFFERENCES, i);
			}
		}
		jsonBuild.append(" }");
		Logger.info("JSON RESULT: " + jsonBuild.toString());
		return jsonBuild.toString();
	}

	/**
	 * Add unique, common, and difference perms for permset to StringBuilder
	 * @param PermissionSet - permset
	 * @param StringBuilder - jsonBuild
	 * @param String - permCategory
	 * @param int - current permset number
	 */
	private static void addCompareResultsToJson(PermissionSet permset,
			StringBuilder jsonBuild, String permCategory, int i) {
		jsonBuild.append(", permset").append(i+1);
		
		Iterator itter = null;
		if (permCategory.equals(UNIQUE)) {
			itter = permset.getUniqueUserPerms().iterator();
			jsonBuild.append(UNIQUE);
			
		} else if (permCategory.equals(COMMON)) {
			itter = permset.getCommonUserPerms().iterator();
			jsonBuild.append(COMMON);
			
		} else if (permCategory.equals(DIFFERENCES)) {
			itter = permset.getDifferenceUserPerms().iterator();
			jsonBuild.append(DIFFERENCES);
		}	
		jsonBuild.append(": [");

		int endOfPermissionsIndex = 11;
		// add each perm to the json result
		while (itter.hasNext()) {
			jsonBuild.append("{\'name': \'");
			
			// 'Permissions' is 11 chars - remove from front and split on Uppercase, not first
			String[] permLableSubstrings = itter.next().toString()
					.substring(endOfPermissionsIndex)
					.split("(?<!^)(?=\\p{Upper})");
			
			for (String substring : permLableSubstrings)
				jsonBuild.append(substring).append(" ");	// splits strings to array, add back
			jsonBuild.append("\', 'enabled': true }");		// currently not used, but just for demo
			if (itter.hasNext()) jsonBuild.append(", ");	// if next, comma, otherwise, no comma
		}
		jsonBuild.append("]");
	}
}
