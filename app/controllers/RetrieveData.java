package controllers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import models.OAuthSession;

import org.apache.commons.lang.StringEscapeUtils;

import play.Logger;
import play.cache.Cache;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Controller;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import controllers.CompareUtils.BaseCompare;

/*
 * Reference: Play-ing in Java - By SANDEEP BHANOT
 * http://blogs.developerforce.com/developer-relations/2011/08/play-ing-in-java.html
 * https://github.com/sbhanot-sfdc/Play-Force
 */
public class RetrieveData extends Controller {
	
	private static final long MEM_THRESHOLD_MB = 10;
	
	/**
	 * Use Force.com API to retrieve data (authentication with OAuth). Returns JsonObject.
	 * @param query - SOQL query string
	 * @return JsonObject with results
	 */
	public static JsonObject query(String query, boolean retry) {
		OAuthSession oauth = ForceDotComOAuth2.getOAuthSession();
		if (oauth == null) {
			Application.index();
		}
		WSRequest req = WS.url(oauth.instance_url
				+ "/services/data/v28.0/query/?q=%s", query);
		req.headers.put("Authorization", "OAuth " + oauth.access_token);
		HttpResponse response = req.get();

		int res = response.getStatus();
		if (res == 200) {
			return response.getJson().getAsJsonObject().getAsJsonObject();
		} else if (res == 400) {
			Logger.info("Response: 400 - Malformed query. Query: %s", query);
		} else if (res == 401) {
			Logger.error("Response: 401 - Session has expired");
		} else if (res == 500) {
			Logger.error("Response 500: Invalid query resulted in response code 500. Query was: %s", query);
		} else {
			Logger.error("Unexpected error. Response: %s", res);
		}
			
		return null;
	}
	
	/**
	 * Retrieve results using Force.com API 
	 * @param itemType - User, PermissionSet, ProfilePermissionSet
	 * @param queryLimit - max number of users to retrieve
	 * @return JsonObject - results
	 */
	public static JsonObject getItems(String itemType, String search, int queryLimit, boolean retry) {
		final JsonObject items = query(generateQuery(itemType, search, queryLimit), retry);
		items.getAsJsonArray("records").forEach(item -> {
			// Need to escape all names to ensure there are no XSS issues
			final JsonObject itemJson = item.getAsJsonObject();
			switch(itemType) {
				case "PermissionSet":
					itemJson.addProperty("Label", 
						StringEscapeUtils.escapeHtml(itemJson.get("Label").getAsString())
					);
					break;
				case "ProfilePermissionSet":
					itemJson.getAsJsonObject("Profile").addProperty("Name", 
						StringEscapeUtils.escapeHtml(itemJson.getAsJsonObject("Profile").get("Name").getAsString())
					);
					break;
				default:
					// default case - only used for User
					itemJson.addProperty("Name", 
						StringEscapeUtils.escapeHtml(itemJson.get("Name").getAsString())
					);
			};
		});
		return items;
	}

	/**
	 * Creates and returns SOQL query string
	 * @param itemType - User, PermissionSet, ProfilePermissionSet
	 * @param itemLimit - number of items to retrieve
	 */
	private static String generateQuery(String itemType, String search, int itemLimit) {
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT Id, ");
		boolean isProfilePermset = itemType.equals("ProfilePermissionSet");

		String displayField = "";
		if (itemType.equals("User")) {
			displayField = "Name";
			queryBuilder.append(displayField).append(" FROM User WHERE IsActive=true ");
		} else if (itemType.equals("PermissionSet")) {
			displayField = "Label";
			queryBuilder.append(displayField).append(" FROM PermissionSet WHERE IsOwnedByProfile=false ");
		} else if (isProfilePermset) { 
			displayField = "Profile.Name";
			queryBuilder.append(displayField).append(" FROM PermissionSet WHERE IsOwnedByProfile=true ");
		}
		
		if (null != search && search.length() > 0) {
			String safeSearch = search.replace("'", "\\'");
			if (!safeSearch.startsWith("%") && !safeSearch.endsWith("%")) {
				// if search string doesn't specify 'starts with' or 'ends with', make generic match string
				safeSearch = "%" + safeSearch + "%";
			}
			queryBuilder.append("AND ").append(displayField).append(" LIKE '").append(safeSearch).append("' ");
		}
		queryBuilder.append("ORDER BY ").append(displayField);
		queryBuilder.append(" ASC NULLS LAST LIMIT ").append(itemLimit);

		Logger.info("QUERY: %s", queryBuilder.toString());
		return queryBuilder.toString();
	}
	

	/**
	 * Calls the Describe method on PermissionSet via the Rest API to get the Permission Fields.
	 * Uses caching and will use cached result, if possible.
	 * 
	 * @param retry
	 */
	public static JsonObject getUserPerms(boolean retry) {
		OAuthSession oauth = ForceDotComOAuth2.getOAuthSession();
		if (oauth == null) {
			Application.index();
		}
		
		String cacheKey = session.getId() + "-userperms";
		String userPerms = Cache.get(cacheKey, String.class);
		if (userPerms != null) {
			// cache hit
			Cache.replace(cacheKey, userPerms, "6mn");	// keep cache warm
			return new JsonParser().parse(userPerms).getAsJsonObject();
		} else {
			// cache miss
			WSRequest req = WS.url(oauth.instance_url
					+ "/services/data/v28.0/sobjects/%s/describe/", "PermissionSet");
			req.headers.put("Authorization", "OAuth " + oauth.access_token);
			HttpResponse response = req.get();
	
			int res = response.getStatus();
			if (res == 200) {
				JsonElement jsonResult = response.getJson();
				Cache.set(cacheKey, jsonResult.toString(), "6mn");
				return jsonResult.getAsJsonObject();
				
			} else if (res == 401) {
				Logger.error("Response: 401 - Session timed out before calling refresh in getUserPerms");
			} else {
				Logger.error("Error occured attempting to retrieve user perms. Response code: %s", res);
			}
		}
		return null;	// shouln't get get here, but need default return
	}
	
	/**
	 * Build SOQL query string for all user permissions
	 * @param permsetId
	 * @param userPerms Set<String> 
	 */
	protected static String userPermQueryBuilder(String permsetId, Set<String> userPerms) {
		StringBuilder queryBuild = new StringBuilder();
		queryBuild.append("SELECT ");

		BaseCompare.appendParamsToQuery(queryBuild, BaseCompare.USER_PERMS, userPerms);
		queryBuild.append(" FROM PermissionSet WHERE Id=\'").append(permsetId).append("\'");

		return queryBuild.toString();
	}
	
	/**
	 * Return JsonObject result of user perms for a specific permset.
	 * Uses caching to prevent more queries than required.
	 * 
	 * @param permsetId
	 * @param userPerms Set<String> 
	 * @param retry
	 */
	public static JsonObject getPermsetUserPerms(String permsetId, Set<String> userPerms, boolean retry) {
		String cacheKey = session.getId() + "-userperms-" + permsetId;
		
		String userPermCacheString = Cache.get(cacheKey, String.class);
		if (userPermCacheString != null) {
			// cache hit
			Cache.replace(cacheKey, userPermCacheString, "3mn");	// keep cache warm
			return new JsonParser().parse(userPermCacheString).getAsJsonObject();
		} else {
			// cache miss
			JsonObject userPermJsonObject = query(userPermQueryBuilder(permsetId, userPerms), retry);
			if (!freeMemoryBelowThreshold()) {
				Cache.set(cacheKey, userPermJsonObject.toString(), "3mn");
			}
			return userPermJsonObject;
		}
	}
	
	/**
	 * Get the SetupEntityAccess Ids for a particular permset.
	 * Uses caching.
	 * 
	 * @param permsetId
	 * @param retry
	 * @return
	 */
	public static List<JsonObject> getSetupAccessIds(String permsetId, boolean retry) {
		List<JsonObject> results = Lists.newArrayList();
		JsonArray resultsArray = new JsonArray();
		
		String cacheKey = session.getId() + "-setupaccess-" + permsetId;
		String setupAccessString = Cache.get(cacheKey, String.class);
		if (setupAccessString != null) {
			// cache hit
			Cache.replace(cacheKey, setupAccessString, "3mn");	// keep cache warm
			resultsArray = new JsonParser().parse(setupAccessString).getAsJsonArray();
		} else {
			// cache miss
			String seaQuery = buildSeaPermQuery(permsetId);
			resultsArray = RetrieveData.query(seaQuery, retry).get("records").getAsJsonArray();			
			if (!freeMemoryBelowThreshold()) {
				Cache.set(cacheKey, resultsArray.toString(), "3mn");
			}
		}
		removeCacheElementIfMemoryLow(cacheKey);
		
		for (JsonElement jsElement : resultsArray) {
			results.add(jsElement.getAsJsonObject());
		}
		return results;
	}
	
	/**
	 * Returns SetupEntityAccess perm SOQL query string
	 * @param parentId - UserId or PermissionSet / ProfileId
	 */
	private static String buildSeaPermQuery(String parentId) {
		StringBuilder query = new StringBuilder("SELECT SetupEntityId FROM SetupEntityAccess WHERE ParentId");
		if (parentId.startsWith(BaseCompare.PERMSET_ID_PREFIX)) {
			query.append("='").append(parentId).append("'");
		} else {
			query.append(" IN (SELECT PermissionSetId from PermissionSetAssignment WHERE ");
			if (parentId.startsWith(BaseCompare.USER_ID_PREFIX)) {
				query.append("AssigneeId='");
			} else if (parentId.startsWith(BaseCompare.PROFILE_ID_PREFIX)) {
				query.append("ProfileId='");
			} else {
				Logger.warn("Invalid parentId prefix.  ParentId: %s", parentId);
			}
			// TODO throw exception - don't allow invalid query string
			query.append(parentId).append("')");
		}
		return query.toString();
	}
	
	/**
	 * Retrieve SetupEntity names / labels.
	 * Uses caching if possible.
	 * 
	 * @param isAppType - Tabset or Connected App
	 * @param apiName
	 * @param idList
	 * @param retry
	 * @return JsonArray results
	 */
	public static List<JsonObject> getSetupEntityNames(boolean isAppType,
			String apiName, ArrayList<String> idList, boolean retry) {
		List<JsonObject> results = Lists.newArrayList();
		List<String> uncachedIds = Lists.newArrayList();
		
		Logger.info("[%s] Number of Ids in idList: %d", apiName, idList.size());

		String cacheKeyPrefix = session.getId() + "-setupentity-";
		// find all cached SEA names and add to results list and determine uncached ids
		for (String id : idList) {
			String cacheKey = cacheKeyPrefix + id;

			String setupEntityNameJson = Cache.get(cacheKey, String.class);
			if (setupEntityNameJson != null) {
				// cache hit - add result to results list
				results.add(new JsonParser().parse(setupEntityNameJson).getAsJsonObject());
				Cache.replace(cacheKey, setupEntityNameJson, "3mn");	// keep cache warm
			} else {
				// cache miss on setup entity name
				uncachedIds.add(id);
			}
		}

		// query to get names for uncached ids
		if (!uncachedIds.isEmpty()) {
			Logger.info("[%s] Number of uncached Ids: %d", apiName, uncachedIds.size());

			// to avoid getting 413 (soql request too large), batch name queries in groups of 200
			int BATCH_SIZE = 200;
			int numNamesToRetrieve = uncachedIds.size();

			while(numNamesToRetrieve > 0) {
				List<String> namesToRetrieve = Lists.newArrayList();
				if (numNamesToRetrieve < BATCH_SIZE) {
					namesToRetrieve.addAll(uncachedIds);
					uncachedIds.clear();
				} else {
					// get batch of names to retrieve
					namesToRetrieve.addAll(uncachedIds.subList(0, BATCH_SIZE));
					uncachedIds.subList(0, BATCH_SIZE).clear();
					
				}
				numNamesToRetrieve = uncachedIds.size();
				String labelQuery = String.format(
						"SELECT Id, %s FROM %s WHERE Id IN (%s)", isAppType ? "Label"
								: "Name", apiName, buildIdListString(namesToRetrieve));
				
				JsonArray uncachedResults = RetrieveData.query(labelQuery, retry)
						.get("records").getAsJsonArray();
				
				// for each entity returned, cache result and add to results list
				for (JsonElement jsonElement : uncachedResults) {
					JsonObject jsonObj = jsonElement.getAsJsonObject();
					if (!freeMemoryBelowThreshold()) {
						Cache.set(cacheKeyPrefix + jsonObj.get("Id").getAsString(), jsonObj.toString(), "3mn");
					}
					results.add(jsonObj);
				}
			}
		}
		
		// results is combination of cached and uncached SetupEntityName JsonObjects
		return results;
	}
	
	/**
	 * Take List<String> and join with single quotes for use in SOQL query
	 * 
	 * @param idList
	 * @return idList String
	 */
	private static String buildIdListString(List<String> idList) {
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
	
	private static void removeCacheElementIfMemoryLow(String cacheKey) {
        if (freeMemoryBelowThreshold()) {
        	Logger.error("Free Memory below threshold. Clearning cache element: %s.", cacheKey);
        	Cache.delete(cacheKey);
        }
	}
	
	/**
	 * Check runtime.freeMemory and check if free memory is less than specified threshold.
	 * @return boolean freeMemoryLessThanThreshold
	 */
	private static boolean freeMemoryBelowThreshold() {
        final long freeMemory = Runtime.getRuntime().freeMemory() / (1024*1024);
        boolean freeMemoryLessThanThreshold = freeMemory < MEM_THRESHOLD_MB;
        if (freeMemoryLessThanThreshold) {
        	Logger.warn("Free Memory less than specified threshold.");
        }
		return freeMemoryLessThanThreshold;
	}
	
}
