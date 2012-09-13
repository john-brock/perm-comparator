package controllers;

import java.util.ArrayList;

import controllers.PermissionSetUtil;
import models.Account;
import models.OAuthSession;
import models.PermissionSet;
import models.User;
import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RetrieveData {
	
	public static ArrayList<Account> getAccounts(boolean retry) {
		Logger.info("getAccounts called");
		OAuthSession oauth = ForceDotComOAuth2.getOAuthSession();
		if (oauth == null)
			return null;

		String query = "select name, id, AccountNumber, AnnualRevenue, NumberOfEmployees from Account limit 10";
		WSRequest req = WS.url(oauth.instance_url
				+ "/services/data/v22.0/query/?q=%s", query);
		req.headers.put("Authorization", "OAuth " + oauth.access_token);
		HttpResponse response = req.get();

		Logger.info("response code is:" + response.getStatus());
		int res = response.getStatus();
		if (res == 200) {
			JsonArray accounts = response.getJson().getAsJsonObject()
					.getAsJsonArray("records");

			if (accounts != null && accounts.size() > 0) {
				ArrayList<Account> accts = new ArrayList<Account>();
				for (int i = 0; i < accounts.size(); i++) {
					JsonObject a = (JsonObject) accounts.get(i);
					Account acct = new Account();
					acct.parseFromJson(a);
					accts.add(acct);
				}
				return accts;
			}
		} else if (res == 401 && retry == false) {
			Logger.info("Calling refresh");
			retry = true;

			ForceDotComOAuth2.refreshToken(
					"https://login.salesforce.com/services/oauth2/token",
					System.getenv("clientKey"), System.getenv("clientSecret"));

			Logger.info("Refresh done");
			getAccounts(retry);
			Logger.info("getAccounts call done");
		}
		return null;
	}

	public static JsonObject getItems(String itemType, int queryLimit, boolean retry) {
		Logger.info("getItems called");
		OAuthSession oauth = ForceDotComOAuth2.getOAuthSession();
		if (oauth == null)
			return null;
		
		String query = generateQuery(itemType, queryLimit);
		Logger.info(query);

		WSRequest req = WS.url(oauth.instance_url
				+ "/services/data/v25.0/query/?q=%s", query);
		req.headers.put("Authorization", "OAuth " + oauth.access_token);
		HttpResponse response = req.get();
		Logger.info("response code is:" + response.getStatus());

		int res = response.getStatus();
		if (res == 200) {
			return response.getJson().getAsJsonObject().getAsJsonObject();
			
		} else if (res == 401 && retry == false) {
			Logger.info("Calling refresh");
			retry = true;

			ForceDotComOAuth2.refreshToken(
					"https://login.salesforce.com/services/oauth2/token",
					System.getenv("clientKey"), System.getenv("clientSecret"));

			Logger.info("Refresh done");
			getItems(itemType, queryLimit, retry);
			Logger.info("getUsers call done");
		}
		return null;
	}

	private static String generateQuery(String itemType, int itemLimit) {
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT Id, ");
		
		if (itemType.equals("User")) 
			queryBuilder.append("Name FROM User ");
		else if (itemType.equals("PermissionSet")) 
			queryBuilder.append("Name FROM PermissionSet WHERE IsOwnedByProfile=false ");
		// Might change to permset Label, but would also need to do mapping on JS store
		else if (itemType.equals("ProfilePermissionSet")) 
			queryBuilder.append("Profile.Name FROM PermissionSet WHERE IsOwnedByProfile=true ");
		queryBuilder.append("LIMIT ").append(itemLimit);
		
		return queryBuilder.toString();
	}
	
	public static JsonObject query(String query, boolean retry) {
		Logger.info("query called");
		OAuthSession oauth = ForceDotComOAuth2.getOAuthSession();
		if (oauth == null)
			Application.index();
		
		Logger.info(query);
		WSRequest req = WS.url(oauth.instance_url
				+ "/services/data/v25.0/query/?q=%s", query);
		req.headers.put("Authorization", "OAuth " + oauth.access_token);
		HttpResponse response = req.get();
		Logger.info("response code is:" + response.getString());
		Logger.info("response code is:" + response.getStatus());
		int res = response.getStatus();
		if (res == 200) {
			return response.getJson().getAsJsonObject().getAsJsonObject();
			
		} else if (res == 401) {
			Logger.info("Calling refresh");
			retry = true;

			ForceDotComOAuth2.refreshToken(
					"https://login.salesforce.com/services/oauth2/token",
					System.getenv("clientKey"), System.getenv("clientSecret"));

			Logger.info("Refresh done");
			query(query, retry);
			Logger.info("query call done");
		}
		return null;
	}
	
	public static PermissionSet makePermset() {
		return PermissionSetUtil.getPermissionSet("0PSE0000000GlLr", true);
	}
}
