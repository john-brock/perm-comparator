package controllers;

import java.util.ArrayList;
import java.util.Set;

import javax.mail.Session;

import controllers.CompareUtils.BaseCompare;
import models.OAuthSession;
import models.PermissionSet;
import play.Logger;
import play.cache.Cache;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/*
 * Reference: Play-ing in Java - By SANDEEP BHANOT
 * http://blogs.developerforce.com/developer-relations/2011/08/play-ing-in-java.html
 * https://github.com/sbhanot-sfdc/Play-Force
 */
public class RetrieveData extends Controller {
	
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
			
		} else if (res == 401) {
			Logger.info("Response: 400 - Calling refresh for query");
			retry = true;

			ForceDotComOAuth2.refreshToken(
					"https://login.salesforce.com/services/oauth2/token",
					System.getenv("clientKey"), System.getenv("clientSecret"));

			Logger.info("Refresh complete for query");
			query(query, retry);
			
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
	public static JsonObject getItems(String itemType, int queryLimit, boolean retry) {
		return  query(generateQuery(itemType, queryLimit), retry);
	}

	/**
	 * Creates and returns SOQL query string
	 * @param itemType - User, PermissionSet, ProfilePermissionSet
	 * @param itemLimit - number of items to retrieve
	 */
	private static String generateQuery(String itemType, int itemLimit) {
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT Id, ");
		
		if (itemType.equals("User")) 
			queryBuilder.append("Name FROM User ORDER BY Name ");
		else if (itemType.equals("PermissionSet")) 
			queryBuilder.append("Label FROM PermissionSet WHERE IsOwnedByProfile=false ORDER BY Name ");
		else if (itemType.equals("ProfilePermissionSet")) 
			queryBuilder.append("Profile.Name FROM PermissionSet WHERE IsOwnedByProfile=true ORDER BY Profile.Name ");
		
		queryBuilder.append("ASC NULLS LAST LIMIT ").append(itemLimit);

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
				Logger.info("Response: 400 - calling refresh in getUserPerms");
				retry = true;
	
				ForceDotComOAuth2.refreshToken(
						"https://login.salesforce.com/services/oauth2/token",
						System.getenv("clientKey"), System.getenv("clientSecret"));
	
				Logger.info("Refresh done for getUserPerms");
				getUserPerms(retry);
				
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
			return new JsonParser().parse(userPermCacheString).getAsJsonObject();
		} else {
			// cache miss
			JsonObject userPermJsonObject = query(userPermQueryBuilder(permsetId, userPerms), retry);
			Cache.set(cacheKey, userPermJsonObject.toString(), "3mn");
			return userPermJsonObject;
		}
	}
	
}
