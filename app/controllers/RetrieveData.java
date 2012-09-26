package controllers;

import java.util.ArrayList;

import controllers.PermissionSetUtil;
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
//		Logger.info("query called");
		OAuthSession oauth = ForceDotComOAuth2.getOAuthSession();
		if (oauth == null)
			Application.index();
		
		WSRequest req = WS.url(oauth.instance_url
				+ "/services/data/v25.0/query/?q=%s", query);
		req.headers.put("Authorization", "OAuth " + oauth.access_token);
		HttpResponse response = req.get();
//		Logger.info("response code is:" + response.getStatus());

		int res = response.getStatus();
		if (res == 200) {
			return response.getJson().getAsJsonObject().getAsJsonObject();
			
		} else if (res == 401) {
//			Logger.info("Calling refresh");
			retry = true;

			ForceDotComOAuth2.refreshToken(
					"https://login.salesforce.com/services/oauth2/token",
					System.getenv("clientKey"), System.getenv("clientSecret"));

//			Logger.info("Refresh done");
			query(query, retry);
//			Logger.info("query call done");
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
}
