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

	private static int QUERY_LIMIT = 200;
	private static boolean retry = false;

	// main function to render login or main page
	public static void index() {
		if ((OAuthSession) Cache.get(session.getId() + "-oauth") != null)
			render("Application/index.html");
		else
			render("Application/login.html");
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
	public static String permsetDiffs(String id1, String id2, String id3, String id4) {
		return PermissionSetUtil.comparePermsets(retry, id1, id2, id3, id4);
	}
	
	
	// courtesy: @sbhanot-sfdc -- https://github.com/sbhanot-sfdc/Play-Force
	public static void sforceLogin() {
		if (!ForceDotComOAuth2.isLoggedIn()) {
			ForceDotComOAuth2.login(System.getenv("clientKey"), System.getenv("clientSecret"),

					new ForceDotComOAuth2.OAuthListner() {
						@Override
						public void onSuccess(OAuthSession session) {
							Logger.info("Session in callback is:" + session);
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
		if (ForceDotComOAuth2.isLoggedIn()) {
			ForceDotComOAuth2.logout();
		}
		index();
	}
}