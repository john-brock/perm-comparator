package controllers;

import java.util.ArrayList;

import controllers.CompareUtils.BaseCompare;
import models.OAuthSession;
import models.PermissionSet;
import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/*
 * Reference: Play-ing in Java - By SANDEEP BHANOT
 * http://blogs.developerforce.com/developer-relations/2011/08/play-ing-in-java.html
 * https://github.com/sbhanot-sfdc/Play-Force
 */
public class RetrieveData {
	
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
			queryBuilder.append("Name FROM PermissionSet WHERE IsOwnedByProfile=false ORDER BY Name ");
		else if (itemType.equals("ProfilePermissionSet")) 
			queryBuilder.append("Profile.Name FROM PermissionSet WHERE IsOwnedByProfile=true ORDER BY Profile.Name ");
		
		queryBuilder.append("ASC NULLS LAST LIMIT ").append(itemLimit);

		return queryBuilder.toString();
	}
	

	/**
	 * Calls the Describe method on PermissionSet via the Rest API to get the Permission Fields
	 */
	public static JsonObject getUserPerms(boolean retry) {
		OAuthSession oauth = ForceDotComOAuth2.getOAuthSession();
		if (oauth == null) {
			Application.index();
		}
		WSRequest req = WS.url(oauth.instance_url
				+ "/services/data/v28.0/sobjects/%s/describe/", "PermissionSet");
		req.headers.put("Authorization", "OAuth " + oauth.access_token);
		HttpResponse response = req.get();

		int res = response.getStatus();
		if (res == 200) {
			return response.getJson().getAsJsonObject().getAsJsonObject();
			
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
		return null;
	}
}
