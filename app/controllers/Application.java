package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.libs.WS.*;
import play.cache.*;
import play.cache.Cache;

import java.util.*;

import javax.media.j3d.ViewSpecificGroup;

import models.*;

import com.google.gson.*;

public class Application extends Controller {

	private static int QUERY_LIMIT = 10;
	private static boolean retry = false;

//	public static void index() {
//		ArrayList<Account> accts = RetrieveData.getAccounts(retry);
//		Logger.info("Retrieved account information from Salesforce.com");
//		renderArgs.put("accounts", accts);
//		render();
//	}

	public static void index() {
		if ((OAuthSession) Cache.get(session.getId() + "-oauth") != null)
			render("Application/test.html");
		else
			render("Application/login.html");
	}
	
	public static void readPermset() {
		PermissionSet permset = RetrieveData.makePermset();
	}
	
	public static void test() {
		render("Application/test.html");
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
	
	// should return Json but creating json-type string and will 'fool' receiver
	public static String permsetDiffs(String id1, String id2, String id3, String id4) {
		return PermissionSetUtil.comparePermsets(retry, id1, id2, id3, id4);
	}
	
	public static void sforceLogin() {
		if (!ForceDotComOAuth2.isLoggedIn()) {

			boolean result = ForceDotComOAuth2.login(
					System.getenv("clientKey"), System.getenv("clientSecret"),

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
	
	public static void renderUserPage() {
		if (!ForceDotComOAuth2.isLoggedIn()) {

			boolean result = ForceDotComOAuth2.login(
					System.getenv("clientKey"), System.getenv("clientSecret"),

					new ForceDotComOAuth2.OAuthListner() {
						@Override
						public void onSuccess(OAuthSession session) {
							Logger.info("Session in callback is:" + session);
							getUsers();
						}

						@Override
						public void onFailure(String error, String errorDesc) {
							renderText("Auth failed" + error);
						}
					});

		}
		getUsers();
	}

	public static void sforceLogout() {
		if (ForceDotComOAuth2.isLoggedIn()) {
			boolean result = ForceDotComOAuth2.logout();
		}
		index();
	}
}