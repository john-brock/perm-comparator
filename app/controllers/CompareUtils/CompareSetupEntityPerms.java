package controllers.CompareUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.map.HashedMap;

import play.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import controllers.RetrieveData;

import models.PermissionSet;
import models.PermissionSet.ObjPermCategory;
import models.PermissionSet.SetupEntityTypes;
import models.PermissionSet.objectPermissions;

public class CompareSetupEntityPerms extends BaseCompare {

	/**
	 * Returns SetupEntityAccess perm SOQL query string
	 * @param parentId - UserId or PermissionSet / ProfileId
	 */
	private static String buildSeaPermQuery(String parentId) {
		StringBuilder query = new StringBuilder("SELECT SetupEntityId FROM SetupEntityAccess WHERE ParentId");
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
//		query.append(" ORDER BY SobjectType ASC NULLS FIRST");
		return query.toString();
	}
	
	public static String compareSetupEntityPerms(boolean retry, String... ids) {
		PermissionSet[] permsets = getPermsetArray(retry, SETUP_ENTITY_PERMS, ids);
		classifySetupEntityPerms(permsets);

		return generatePermsJson(permsets, SETUP_ENTITY_PERMS);
	}

	protected static void addPermsToPermset(PermissionSet permset, boolean retry) {
		String seaQuery = buildSeaPermQuery(permset.getId());
		Logger.info("Add SEA Query: %s", seaQuery);
		JsonArray seaInfo = RetrieveData.query(seaQuery, retry).get("records").getAsJsonArray();
		if (seaInfo == null) {
			Logger.warn("PermsetInfo is null after query in getPermissionSet. Query: %s", seaQuery);
		}
		// Logger.warn("JSON ARRAY (SEA INFO) %s", seaInfo.toString());
		// add all setupEntityIds to list to retrieve entity names
		Map<String, ArrayList<String>> typeToEntityMap = Maps.newHashMap();
		for (SetupEntityTypes type : SetupEntityTypes.values()) {
			typeToEntityMap.put(type.getPrefix(), Lists.<String>newArrayList());
		}
		for (JsonElement sea : seaInfo) {
			String id = sea.getAsJsonObject().get("SetupEntityId").getAsString();
			// add the entity id to the list corresponding to the entity type
			String prefix = id.substring(0, 3);
			ArrayList<String> newIdList = typeToEntityMap.get(prefix);
			if (newIdList != null) {
				newIdList.add(id);
				typeToEntityMap.put(prefix, newIdList);
			}
		}
		//Logger.warn("ENTITY MAP: %s", typeToEntityMap.toString());
		for (SetupEntityTypes type : SetupEntityTypes.values()) {
			boolean isAppType = (type.equals(SetupEntityTypes.CONN_APP) || type
					.equals(SetupEntityTypes.TABSET));
			String labelQuery = String.format(
					"SELECT %s FROM %s WHERE Id IN (%s)", isAppType ? "Label"
							: "Name", type.getFieldName(),
					buildIdListString(typeToEntityMap.get(type.getPrefix())));
			//Logger.warn("Query: %s", labelQuery);
			JsonArray results = RetrieveData.query(labelQuery, retry).get("records").getAsJsonArray();
			if (results == null) {
				Logger.warn("Type: %s skipped", type.getFieldName());
				continue;
			}
			for (JsonElement result : results) {
				permset.getSeaPermMap(ObjPermCategory.original)
						.get(type)
						.add(result.getAsJsonObject()
								.get(isAppType ? "Label" : "Name")
								.getAsString());
			}
		}
	}
	
	private static String buildIdListString(ArrayList<String> idList) {
		StringBuilder builder = new StringBuilder();
		if (idList.isEmpty()) {
			return "\'\'";
		}
		Iterator itter = idList.iterator();
		while (itter.hasNext()) {
			builder.append("\'" + itter.next() + "\'");
			if (itter.hasNext()) { builder.append(","); }
		}
		return builder.toString();
	}
	
	/**
	 * Performs comparison operations on effective SEA perms to find Unique, Common, and Differences.
	 * Sets respective EnumSet on the permission set object
	 * @param PermissionSet[] permsets
	 */
	public static void classifySetupEntityPerms(PermissionSet[] permsets) {
		int numberOfPermsets = permsets.length;

		for (int i=0; i<numberOfPermsets; i++) {
			if (permsets[i] != null) {
				// start with copy of user perms
				Map<SetupEntityTypes, Set<String>> origMap = Maps.newHashMap(ImmutableMap.copyOf(permsets[i].getSeaPermMap(ObjPermCategory.original)));
				Map<SetupEntityTypes, Set<String>> uniquePerms = getNewHashMap(origMap);
				Map<SetupEntityTypes, Set<String>> commonPerms = getNewHashMap(origMap);
				
				Set<SetupEntityTypes> uniqueKeysToRemove = Sets.newHashSet();
				Set<SetupEntityTypes> commonKeysToRemove = Sets.newHashSet();
				
				for (int j = 0; j < numberOfPermsets; j++) {
					// ensure not comparing to permset to itself (will compare if placed in toolbar twice)
					if ((permsets[j] != null) && (j != i)) {
						Map<SetupEntityTypes, Set<String>> compPermsetMap = permsets[j].getSeaPermMap(ObjPermCategory.original);
						
						for (SetupEntityTypes type : SetupEntityTypes.values()) {
							// unique perms = set of perms minus intersection with each other permset
							if (compPermsetMap.containsKey(type)) {
								if (uniquePerms.containsKey(type)) {
									uniquePerms.get(type).removeAll(compPermsetMap.get(type));
									if (uniquePerms.get(type).isEmpty()) { uniqueKeysToRemove.add(type); }
								}
							}
							if (commonPerms.containsKey(type) && compPermsetMap.containsKey(type)) {
								// common perms finds the intersection with every other permset
								commonPerms.get(type).retainAll(compPermsetMap.get(type));
								if (commonPerms.get(type).isEmpty()) {
									commonKeysToRemove.add(type);
								}
							} else {
								commonKeysToRemove.add(type);
							}
						}
					}
				}
				removeKeys(uniquePerms, uniqueKeysToRemove);
				permsets[i].setSeaPermMap(ObjPermCategory.unique, uniquePerms);

				removeKeys(commonPerms, commonKeysToRemove);
				permsets[i].setSeaPermMap(ObjPermCategory.common, commonPerms);
				
				// difference perms is simply perms minus intersection with all others (common perms)
				Map<SetupEntityTypes, Set<String>> differencePerms = getNewHashMap(origMap);
				Set<SetupEntityTypes> keysToRemove = Sets.newHashSet();
				for (SetupEntityTypes type : commonPerms.keySet()) {
					if (differencePerms.containsKey(type)) {
						differencePerms.get(type).removeAll(commonPerms.get(type));
						if (differencePerms.get(type).isEmpty()) {
							keysToRemove.add(type);
						}
					}
				}
				// remove the keys that do not have any entries and set difference perms on permset
				removeKeys(differencePerms, keysToRemove);
				permsets[i].setSeaPermMap(ObjPermCategory.differing, differencePerms);
			}
		}
	}
	
	private static void removeKeys(Map<SetupEntityTypes, Set<String>> map, Set<SetupEntityTypes> typesToRemove) {
		for (SetupEntityTypes type : typesToRemove) {
			map.remove(type);
		}	
	}

	/**
	 * Utility method to copy the map of SetupEntityAccess perms
	 */
	private static Map<SetupEntityTypes, Set<String>> getNewHashMap(final Map<SetupEntityTypes, Set<String>> mapToCopy) {
		Map<SetupEntityTypes, Set<String>> newMap = Maps.newHashMap();
		for (SetupEntityTypes key : mapToCopy.keySet()) {
			newMap.put(key, Sets.newHashSet(mapToCopy.get(key)));
		}
		return newMap;
	}
	
	/**
	 * Add unique, common, and difference SEA perms for permset to StringBuilder
	 * @param PermissionSet - permset
	 * @param StringBuilder - jsonBuild
	 * @param String - permCategory
	 * @param int - current permset number
	 */
	public static void addSEAPermResultsToJson(PermissionSet permset,
			StringBuilder jsonBuild, String permCategory, int i) {
		Iterator entityItter = null;
		Iterator seaItter = null;

		SetupEntityTypes seaName = null;
		Map<SetupEntityTypes, Set<String>> seaPermMap = new HashMap();

		StringBuilder permsetRoot = new StringBuilder();
		permsetRoot.append("permset").append(i+1).append(SETUP_ENTITY_PERMS);
		//permsetRoot.append("permset").append(i+1).append(OBJECT);
		if (permCategory.equals(UNIQUE)) {
			seaPermMap = permset.getSeaPermMap(ObjPermCategory.unique);
			permsetRoot.append(UNIQUE);
			
		} else if (permCategory.equals(COMMON)) {
			seaPermMap = permset.getSeaPermMap(ObjPermCategory.common);
			permsetRoot.append(COMMON);
			
		} else if (permCategory.equals(DIFFERENCES)) {
			seaPermMap = permset.getSeaPermMap(ObjPermCategory.differing);
			permsetRoot.append(DIFFERENCES);
		}
		
		String permsetLabel = permsetRoot.toString();
		jsonBuild.append(", ").append("\"" + permsetLabel + "\"").append(": [");
		
		SortedSet<SetupEntityTypes> alphabeticalKeySet = new TreeSet<SetupEntityTypes>();
		for (SetupEntityTypes type : seaPermMap.keySet()) {
			alphabeticalKeySet.add(type);
		}
		
		// TODO -- need to loop through each of the sets associated with each SEAType and add all names
		// to the json for that particular SEAType.
		seaItter = alphabeticalKeySet.iterator();
		if (seaItter.hasNext()) {jsonBuild.append("{ \"success\": \"true\", \"text\": \"Setup Entities\", \"").append(permsetLabel).append("\": [ "); }
		while (seaItter.hasNext()) {
			seaName = (SetupEntityTypes) seaItter.next();
			jsonBuild.append("{\"success\": \"true\", \"text\": \"").append(seaName.getDisplayName()).append("\", \"").append(permsetLabel).append("\": [");

			if (seaPermMap.containsKey(seaName)) {
				entityItter = seaPermMap.get(seaName).iterator();
			} else {
				continue;
			}
			
			while (entityItter.hasNext()) {
				jsonBuild.append("{\"success\": \"true\", \"text\": \"");
				jsonBuild.append(entityItter.next().toString());
				jsonBuild.append("\", ").append("\"leaf\":\"true\", \"icon\":\"../../resources/themes/images/default/tree/checkMark.png\", \"loaded\":\"true\" }");			// currently not used, but just for demo
				if (entityItter.hasNext()) { jsonBuild.append(", "); }	// if next, comma, otherwise, no comma
			} 
			jsonBuild.append("], \"leaf\":\"false\", \"expanded\":\"true\", \"loaded\":\"true\"}");
			if (seaItter.hasNext()) { 
				jsonBuild.append(", "); 
			} else {
				jsonBuild.append(" ], \"leaf\":\"false\", \"expanded\":\"true\", \"loaded\":\"true\"}");
			}
		}
		jsonBuild.append("]");
	}

}
