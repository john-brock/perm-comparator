package controllers.CompareUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import play.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import controllers.RetrieveData;


import models.PermissionSet;
import models.PermissionSet.ObjPermCategory;
import models.PermissionSet.SetupEntityTypes;

public class CompareUserPerms extends BaseCompare {

	
	public static String compareUserPerms(boolean retry, String... ids) {
		PermissionSet[] permsets = getPermsetArray(retry, USER_PERMS, ids);
		classifyUserPerms(permsets);

		return generatePermsJson(permsets, USER_PERMS);
	}
	
	protected static Set<String> retrieveValidUserPerms() {
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
	 * Add permissions that are set to true for the PermissionSet
	 * @param permset
	 * @param permsetInfo
	 */
	protected static void addPermsToPermset(PermissionSet permset, boolean retry) {
		Set<String> userPerms = retrieveValidUserPerms();

		JsonObject permsetInfo = RetrieveData.getPermsetUserPerms(permset.getId(), userPerms, retry).get("records").getAsJsonArray().get(0).getAsJsonObject();
		if (permsetInfo == null) {
			Logger.warn("PermsetInfo is null after query in getPermissionSet.");
		}
		
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
	 * Add unique, common, and difference user perms for permset to StringBuilder
	 * @param PermissionSet - permset
	 * @param StringBuilder - jsonBuild
	 * @param String - permCategory
	 * @param int - current permset number
	 */
	public static void addUserPermResultsToJson(PermissionSet permset,
			StringBuilder jsonBuild, String permCategory, int i) {
		jsonBuild.append(", \"permset").append(i+1);
		
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
		
		jsonBuild.append("\": [");

		Set<String> sortedPerms = new TreeSet<String>(userPerms);
		itter = sortedPerms.iterator();

		int endOfPermissionsIndex = 11;
		// add each perm to the json result
		while (itter.hasNext()) {
			jsonBuild.append("{\"name\": \"");
			
			// 'Permissions' is 11 chars - remove from front and split on Uppercase, not first
			String[] permLableSubstrings = itter.next().toString()
					.substring(endOfPermissionsIndex)
					.split("(?<!^)(?=\\p{Upper})");
			
			for (String substring : permLableSubstrings) {
				jsonBuild.append(substring).append(" ");	// splits strings to array, add back
			}
			jsonBuild.append("\", \"enabled\": true }");	// currently not used, but just for demo
			if (itter.hasNext()) jsonBuild.append(", ");	// if next, comma, otherwise, no comma
		}
		jsonBuild.append("]");
	}
}
