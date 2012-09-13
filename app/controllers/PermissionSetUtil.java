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
	
	public static PermissionSet getPermissionSet(String permsetId, boolean retry) {
		PermissionSet permset = new PermissionSet(permsetId, "test");

		String query = queryBuilder(permsetId);
		Logger.info(query);
		JsonObject permsetInfo = RetrieveData.query(query, retry).get("records").getAsJsonArray().get(0).getAsJsonObject();
		Logger.info("ARRAYValues: " + permsetInfo.toString());

		// get permissions available to set in PermissionSet enum
		for (validUserPerms perm : PermissionSet.validUserPerms.values()) {
			String permName = perm.toString();
			if (permsetInfo.has(permName)) {
				// if permission is allowed (true) - add to enumSet
				if (permsetInfo.get(permName).getAsBoolean() == true)
					permset.getUserPerms().add(perm);
			}
		}
		Logger.info(permset.getUserPerms().toString());
		return permset;
	}
	
	public static ImmutableSet<String> getUserPermsetIds(String userId, boolean retry) {
		ImmutableSet.Builder<String> idsBuilder = new ImmutableSet.Builder<String>();
		String query = "SELECT PermissionSet.Id FROM PermissionSetAssignment WHERE AssigneeId='" + userId + "'";

		JsonArray permsetInfo = RetrieveData.query(query, retry).get("records").getAsJsonArray();
		Logger.info("JSON User Result: " + permsetInfo.toString());
		for (JsonElement returnedPermset : permsetInfo) {
			idsBuilder.add(returnedPermset.getAsJsonObject().get("PermissionSet").getAsJsonObject().get("Id").getAsString());
		}
		return idsBuilder.build();
	}
	
	public static PermissionSet aggregatePermissionSets(ImmutableSet<PermissionSet> permsets) {
		PermissionSet permset = new PermissionSet("aggregatePermset_FakeId");
		EnumSet<PermissionSet.validUserPerms> aggregatePerms = EnumSet.noneOf(PermissionSet.validUserPerms.class);

		Iterator permIter = permsets.iterator();
		while (permIter.hasNext()) {
			aggregatePerms.addAll(((PermissionSet) permIter.next()).getUserPerms());
		}
		permset.setUserPerms(aggregatePerms);
		Logger.info("PERMSET SET: " + permset.getUserPerms().toString());

		return permset;
	}
	
	public static String comparePermsets(boolean retry, String... ids) {
		PermissionSet[] permsets = getPermsetArray(retry, ids);
		findUniqueAndCommonPerms(permsets);
		
		return generatePermsJson(permsets);
	}

	private static PermissionSet[] getPermsetArray(boolean retry, String... ids) {
		int numberOfIds = ids.length;
		PermissionSet[] permsets = new PermissionSet[numberOfIds];
		
		for (int i=0; i < numberOfIds; i++) {
			if (ids[i].contains("blank"))
				permsets[i] = null;
			else {
				if (ids[i].startsWith("005E")) {
					permsets[i] = getEffectiveUserPermset(ids[i]);
				}
				else
					permsets[i] = getPermissionSet(ids[i], retry);
			}
		}
		return permsets;
	}
	
	private static PermissionSet getEffectiveUserPermset(String userId) {
		ImmutableSet.Builder<PermissionSet> permsetSetBuilder = new ImmutableSet.Builder<PermissionSet>();
		boolean retry = true;
		
		ImmutableSet<String> permsetIds = getUserPermsetIds(userId, retry);
		Iterator ittr = permsetIds.iterator();
		while (ittr.hasNext())
			permsetSetBuilder.add(getPermissionSet(ittr.next().toString(), retry));
		
		return aggregatePermissionSets(permsetSetBuilder.build());
	}
	
	private static void findUniqueAndCommonPerms(PermissionSet[] permsets) {
		int numberOfPermsets = permsets.length;

		for (int i=0; i<numberOfPermsets; i++) {
			if (permsets[i] != null) {
				EnumSet<PermissionSet.validUserPerms> uniquePerms = EnumSet.copyOf(permsets[i].getUserPerms());
				EnumSet<PermissionSet.validUserPerms> commonPerms = EnumSet.copyOf(permsets[i].getUserPerms());
				for (int j=0; j<numberOfPermsets; j++) {
					if ((permsets[j] != null) && (j != i)) {
						uniquePerms.removeAll(permsets[j].getUserPerms());
						commonPerms.retainAll(permsets[j].getUserPerms());
					}
				}
				permsets[i].setUniqueUserPerms(uniquePerms);
				permsets[i].setCommonUserPerms(commonPerms);
			}
		}
	}
	
	// Return Json representation of permissions for each permset in array
	private static String generatePermsJson(PermissionSet[] permsets) {
		int numberOfPermsets = permsets.length;
		
		StringBuilder jsonBuild = new StringBuilder();
		jsonBuild.append("{\'numberOfPermsets\': ").append(numberOfPermsets);
		
		for (int i=0; i<numberOfPermsets; i++) {
			if (permsets[i] != null) {
				addPermsetCompareResults(permsets, jsonBuild, true, i);
				addPermsetCompareResults(permsets, jsonBuild, false, i);
			}
		}
		jsonBuild.append(" }");
		Logger.info("JSON RESULT: " + jsonBuild.toString());
		return jsonBuild.toString();
	}

	private static void addPermsetCompareResults(PermissionSet[] permsets,
			StringBuilder jsonBuild, boolean addUnique, int i) {
		Iterator itter = addUnique ? permsets[i].getUniqueUserPerms().iterator() : 
			permsets[i].getCommonUserPerms().iterator();
		jsonBuild.append(", permset").append(i+1).append(addUnique ? "_Unique" : "_Common").append(": [");
		while (itter.hasNext()) {
			jsonBuild.append("{\'name': \'").append(itter.next().toString()).append("\', 'enabled': true }");
			if (itter.hasNext()) jsonBuild.append(", ");
		}
		jsonBuild.append("]");
	}
}
