package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.libs.WS.*;
import play.cache.*;
import play.cache.Cache;

import java.util.*;

import models.*;

import com.google.gson.*;

public class Application extends Controller {

	private static int QUERY_LIMIT = 500;
	private static boolean retry = false;

	// main function to render login or main page
	public static void index() {
		if ((OAuthSession) Cache.get(session.getId() + "-oauth") != null) {
			Logger.info("OAuth success - rendering index.html");
			render("Application/index.html");
		} else {
			Logger.info("No OAuth session in cache - rendering login.html");
			render("Application/login.html");
		}
	}
	
	public static JsonObject getUsers() {
		return RetrieveData.getItems("User", QUERY_LIMIT, retry);
	}
	
	public static JsonObject getPermsets() {
		return RetrieveData.getItems("PermissionSet", QUERY_LIMIT, retry);
	}

	public static JsonObject getProfilePermsets() {
		return RetrieveData.getItems("ProfilePermissionSet", QUERY_LIMIT, retry);
	}
	
	// return string that looks like a Json response
	public static String userPermDiffs(String id1, String id2, String id3, String id4) {
		Logger.info("Diffs - id1: %s, id2: %s, id3: %s, id4: %s",  id1, id2, id3, id4);
		return PermissionSetUtil.comparePermsets(retry, id1, id2, id3, id4);
	}
	
	// return string that looks like a Json response for object perm differences
	public static String objectPermDiffs(String id1, String id2, String id3, String id4) {
//		Logger.info("objectPermDiffs - id1: %s, id2: %s, id3: %s, id4: %s",  id1, id2, id3, id4);
		return PermissionSetUtil.compareObjPerms(retry, id1, id2, id3, id4);
	}
	
	// courtesy: @sbhanot-sfdc -- https://github.com/sbhanot-sfdc/Play-Force
	public static void sforceLogin() {
		if (!ForceDotComOAuth2.isLoggedIn()) {
			ForceDotComOAuth2.login(System.getenv("clientKey"), System.getenv("clientSecret"),

					new ForceDotComOAuth2.OAuthListner() {
						@Override
						public void onSuccess(OAuthSession session) {
							index();
						}

						@Override
						public void onFailure(String error, String errorDesc) {
							renderText("Auth failed" + error);
						}
					});
		}
		index();
	}

	public static void sforceLogout() {
		String instanceURL = null;
		
		if (ForceDotComOAuth2.isLoggedIn()) {
			instanceURL = ForceDotComOAuth2.getOAuthSession().instance_url;
			ForceDotComOAuth2.logout();
		}
		
		String redirect = "3;URL=/";	// redirect string - default 3 seconds
		if (instanceURL != null) {
			String didSucceed = "Successful!";
			instanceURL += "/secur/logout.jsp";
			render("Application/logout.html", instanceURL, redirect, didSucceed);
			
		} else {
			Logger.error("instanceURL was null on logout");
			instanceURL = "";		// set empty string as instanceURL
			redirect = "10;URL=/";	// set longer redirect timeout since logout failed
			render("Application/logout.html", instanceURL, redirect);
		}
	}
}