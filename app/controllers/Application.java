package controllers;

import java.util.Map;

import models.OAuthSession;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Util;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import controllers.CompareUtils.CompareObjectPerms;
import controllers.CompareUtils.CompareSetupEntityPerms;
import controllers.CompareUtils.CompareUserPerms;

public class Application extends Controller {

	private static int QUERY_LIMIT = 500;
	private static boolean retry = false;
	private static final String sandboxParam = "sandboxLogin";

	private static final Map<String, Boolean> CONFIG_MAP = ImmutableMap.of(
		"shouldShowId", Boolean.valueOf(System.getenv("shouldShowId"))
	);
	private static final JsonObject CONFIG_JSON = buildConfig(CONFIG_MAP);

    /** Called before every request to ensure that HTTPS is used. */
	/** redirect code credit: http://stackoverflow.com/questions/7415030/enforce-https-routing-for-login-with-play-framework **/
    @Before
    public static void redirectToHttps() {
        //if it's not secure, but Heroku has already done the SSL processing then it might actually be secure after all
        if (!request.secure && request.headers.get("x-forwarded-proto") != null) {
            request.secure = request.headers.get("x-forwarded-proto").values.contains("https");
        }

        //redirect if it's not secure
        if (!request.secure) {
            String url = redirectHostHttps() + request.url;
            System.out.println("Redirecting to secure: " + url);
            redirect(url);
        }
    }

    /** Renames the host to be https://, handles both Heroku and local testing. */
    @Util
    public static String redirectHostHttps() {
        if (Play.id.equals("dev")) {
            String[] pieces = request.host.split(":");
            String httpsPort = (String) Play.configuration.get("https.port");
            return "https://" + pieces[0] + ":" + httpsPort; 
        } else {
            if (request.host.endsWith("domain.com")) {
                return "https://secure.domain.com";
            } else {
                return "https://" + request.host;
            }
        }
    }  
	
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
	
	public static JsonObject getConfigs() {
		return CONFIG_JSON;
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
		
		// remove user perms from cache on logout
		Cache.delete(session.getId() + "-userperms");
		
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

	private static JsonObject buildConfig(final Map<String, Boolean> configMap) {
		final JsonArray configs = new JsonArray();
		for (String key : CONFIG_MAP.keySet()) {
			final JsonObject shouldShowId = new JsonObject();
			shouldShowId.addProperty("key", key);
			shouldShowId.addProperty("value", CONFIG_MAP.get(key));
			configs.add(shouldShowId);
		}
		
		final JsonObject toReturn = new JsonObject();
		toReturn.addProperty("done", true);
		toReturn.addProperty("totalSize", configs.size());
		toReturn.add("configs", configs);
		return toReturn;
	}	
}