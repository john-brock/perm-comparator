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

import controllers.CompareUtils.CompareObjectPerms;
import controllers.CompareUtils.CompareSetupEntityPerms;
import controllers.CompareUtils.CompareUserPerms;

public class Application extends Controller {

	private static int QUERY_LIMIT = 500;
	private static boolean retry = false;
	private static final String sandboxParam = "sandboxLogin";

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
	
	public static JsonObject getUsers(String search) {
		if (null != search && search.length() > 0) { Logger.info("Search : %s", search); }
		return RetrieveData.getItems("User", search, QUERY_LIMIT, retry);
	}
	
	public static JsonObject getPermsets(String search) {
		return RetrieveData.getItems("PermissionSet", search, QUERY_LIMIT, retry);
	}

	public static JsonObject getProfilePermsets(String search) {
		return RetrieveData.getItems("ProfilePermissionSet", search, QUERY_LIMIT, retry);
	}
	
	// return string that looks like a Json response
	public static String userPermDiffs(String id1, String id2, String id3, String id4) {
		Logger.info("Diffs - id1: %s, id2: %s, id3: %s, id4: %s",  id1, id2, id3, id4);
		
		long timestart = System.currentTimeMillis();
		String returnString = CompareUserPerms.compareUserPerms(retry, id1, id2, id3, id4);
		long endtime = System.currentTimeMillis();
		Logger.info("UserPerm comp took: %d milliseconds", (endtime - timestart));

		return returnString;
	}
	
	// return string that looks like a Json response for object perm differences
	public static String objectPermDiffs(String id1, String id2, String id3, String id4) {
		Logger.info("objectPermDiffs - id1: %s, id2: %s, id3: %s, id4: %s",  id1, id2, id3, id4);

		long timestart = System.currentTimeMillis();
		String returnString = CompareObjectPerms.compareObjPerms(retry, id1, id2, id3, id4);
		long endtime = System.currentTimeMillis();
		Logger.info("ObjectPerm comp took: %d milliseconds", (endtime - timestart));

		return returnString;
	}

	// return string that looks like a Json response for SetupEntityAccess perm differences
	public static String setupEntityPermDiffs(String id1, String id2, String id3, String id4) {
		Logger.info("setupEntityPermDiffs - id1: %s, id2: %s, id3: %s, id4: %s",  id1, id2, id3, id4);

		long timestart = System.currentTimeMillis();
		String returnString = CompareSetupEntityPerms.compareSetupEntityPerms(retry, id1, id2, id3, id4);
		long endtime = System.currentTimeMillis();
		Logger.info("SEAPerm comp took: %d milliseconds", (endtime - timestart));
		
		return returnString;		
	}
	
	// courtesy: @sbhanot-sfdc -- https://github.com/sbhanot-sfdc/Play-Force
	public static void sforceLogin() {
		String sBoxParam = params.get(sandboxParam);
		boolean sandboxLogin = sBoxParam == null ? false : sBoxParam.contains("true");
		if (sandboxLogin) { Logger.info("Sandbox login"); }
		if (!ForceDotComOAuth2.isLoggedIn()) {
			ForceDotComOAuth2.login(System.getenv("clientKey"), System.getenv("clientSecret"),
					new ForceDotComOAuth2.OAuthListner() {
						@Override
						public void onSuccess(OAuthSession session) {
							index();
						}

						@Override
						public void onFailure(String error, String errorDesc) {
							Logger.error(
									"Error: %s. Description: %s",
									error, errorDesc);
							renderText(String
									.format("OAuth was not able to complete authentication process. \n\nError: %s. Description: %s",
											error, errorDesc));
						}
					}, 
					sandboxLogin);
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
			Logger.info("Logout success");
			
			render("Application/logout.html", instanceURL, redirect, didSucceed);
			
		} else {
			Logger.error("Logout failed. InstanceURL was null on logout");
			instanceURL = "";		// set empty string as instanceURL
			redirect = "10;URL=/";	// set longer redirect timeout since logout failed
			
			render("Application/logout.html", instanceURL, redirect);
		}
	}
}