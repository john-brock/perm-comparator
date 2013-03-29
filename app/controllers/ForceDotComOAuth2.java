package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.cache.*;

import java.util.*;
import play.mvc.results.Redirect;
import play.mvc.Scope.Params;
import play.mvc.Http.Response;

import models.*;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import play.libs.WS.HttpResponse;
import java.io.Serializable;

/*
 * Play-ing in Java - By SANDEEP BHANOT
 * http://blogs.developerforce.com/developer-relations/2011/08/play-ing-in-java.html
 * https://github.com/sbhanot-sfdc/Play-Force
 */
public class ForceDotComOAuth2 extends Controller {

	private static final String AUTH_URL = "https://login.salesforce.com/services/oauth2/authorize";
	private static final String TOKEN_URL = "https://login.salesforce.com/services/oauth2/token";
	private static final String REVOKE_URL = "https://login.salesforce.com/services/oauth2/revoke";

	private static final String AUTH_URL_sBox = "https://test.salesforce.com/services/oauth2/authorize";
	private static final String TOKEN_URL_sBox = "https://test.salesforce.com/services/oauth2/token";
	private static final String REVOKE_URL_sBox = "https://test.salesforce.com/services/oauth2/revoke";
	
	private static final String ACCESS_TOKEN = "access_token";
	private static final String REFRESH_TOKEN = "refresh_token";
	private static final String ID_ATTR = "id";
	private static final String INSTANCE_URL = "instance_url";
	private static final String SIGNATURE = "signature";
	
	private static String clientid;
	private static String secret;

	public static boolean isLoggedIn() {
		return (getOAuthSession() != null) ? true : false;
	}

	public static OAuthSession getOAuthSession() {
		OAuthSession sess = (OAuthSession) Cache
				.get(session.getId() + "-oauth");
		return sess;
	}

	public static boolean login(String clientId, String sec, OAuthListner list, boolean sandboxLogin) {
		return login(sandboxLogin ? AUTH_URL_sBox : AUTH_URL,
				sandboxLogin ? TOKEN_URL_sBox : TOKEN_URL, clientId, sec, list);
	}

	public static boolean login(String authURL, String tokenURL,
			String clientId, String sec, OAuthListner list) {
		if (list == null)
			return false;

		clientid = clientId;
		secret = sec;

		OAuthSession sess = (OAuthSession) Cache
				.get(session.getId() + "-oauth");
		if (sess == null) {
			if (isPersistentSession()) {
				Http.Cookie userId = request.cookies.get("uid");

				if (userId != null && userId.value != null) {
					String id = userId.value.substring(0,
							userId.value.indexOf("-"));
					String sign = userId.value.substring(userId.value
							.indexOf("-") + 1);

					if (Crypto.sign(id).equals(sign)) {
						sess = OAuthSession.get(id);
						if (sess == null) {
							response.removeCookie("uid");
							initiateOAuthFlow(list, authURL);
						}
					} else {
						response.removeCookie("uid");
						initiateOAuthFlow(list, authURL);
					}
				} else {
					initiateOAuthFlow(list, authURL);
				}
			} else {
				initiateOAuthFlow(list, authURL);
			}
		}

		Cache.set(session.getId() + "-oauth", sess);
		list.onSuccess(sess);
		return true;
	}

	public static boolean logout() {
		response.removeCookie("uid");
		OAuthSession sess = (OAuthSession) Cache
				.get(session.getId() + "-oauth");
		boolean sandboxLogout = (Boolean) Cache.get(session.getId() + "-sandbox");
		Map<String, Object> params = new HashMap();
		params.put("token", sess.access_token);
		HttpResponse revokeResponse = WS.url(sandboxLogout ? REVOKE_URL_sBox : REVOKE_URL).params(params).post();
		if (!revokeResponse.success()) {
			Logger.info("revoke response failed: " + revokeResponse.toString());
		}
		if (sess != null) {
			Cache.safeDelete(session.getId() + "-oauth");
//			Logger.info("cache removed in logout:");
//			OAuthSession u = OAuthSession.get(sess.uid);
//			Logger.info("persistent session in logout:" + u);
//			if (u != null) {
//				u.delete();
//				Logger.warn("persistent session should not be present");
//			}
		}
		return true;
	}

	private static boolean isPersistentSession() {
		String persistent = Play.configuration
				.getProperty("sfdc.persistentSession");
		return (persistent != null && persistent.equals("true")) ? true : false;
	}

	private static void initiateOAuthFlow(OAuthListner list, String authURL) {
//		Logger.info("initiateOAuthFlow called");
		Cache.set(session.getId() + "-listener", list);
		throw new Redirect(authURL
				+ "?response_type=code&client_id="
				+ clientid
				+ "&state="
				+ (authURL.contains("test.salesforce.com") ? "sandbox" : "production")
				+ "&redirect_uri="
				+ WS.encode(play.mvc.Router
						.getFullUrl("ForceDotComOAuth2.callback")));
	}

	public static void callback() {
//		Logger.info("callback called");
		OAuthListner listener = (OAuthListner) Cache.get(session.getId()
				+ "-listener");
		if (listener == null)
			return;

		if (!Cache.safeDelete(session.getId() + "-listener")) {
			Logger.error("Could not remove from cache");
			return;
		}

		String accessCode = Params.current().get("code");
		
		String sandboxParam = Params.current().get("state");
		boolean sandboxLogin = false;
		if (sandboxParam != null) {	// defensive null check, should always have value
			sandboxLogin = sandboxParam.contains("sandbox");
		}

		if (accessCode == null) {
			Logger.error("Callback failed. AccessCode parameter was not present. Usually from 'Deny' access.");
			listener.onFailure(Params.current().get("error"), Params.current()
					.get("error_description"));
		} else {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("grant_type", "authorization_code");
			params.put("client_id", clientid);
			params.put("client_secret", secret);
			params.put("redirect_uri",
					play.mvc.Router.getFullUrl("ForceDotComOAuth2.callback"));
			params.put("code", accessCode);
			HttpResponse response = WS.url(sandboxLogin ? TOKEN_URL_sBox : TOKEN_URL).params(params).post();
			JsonObject r = response.getJson().getAsJsonObject();

			// ensure all expected elements are present in response object
			if (attributesPresent(r, Lists.newArrayList(ACCESS_TOKEN,
					REFRESH_TOKEN, ID_ATTR, INSTANCE_URL, SIGNATURE))) {
				
				OAuthSession s = new OAuthSession();
				s.access_token = r.getAsJsonPrimitive(ACCESS_TOKEN).getAsString();
				s.refresh_token = r.getAsJsonPrimitive(REFRESH_TOKEN).getAsString();
				s.idURL = r.getAsJsonPrimitive(ID_ATTR).getAsString();
				s.instance_url = r.getAsJsonPrimitive(INSTANCE_URL).getAsString();
				s.signature = r.getAsJsonPrimitive(SIGNATURE).getAsString();

				String id = s.idURL.substring(s.idURL.lastIndexOf('/') + 1);
				s.uid = id;
				
				Cache.set(session.getId() + "-oauth", s);
				Cache.set(session.getId() + "-sandbox", sandboxLogin);
				
				if (isPersistentSession()) {
					Response.current().setCookie("uid",
							id + "-" + Crypto.sign(id), "30d");
					s.save();
				}
				listener.onSuccess(s);
			} else {
				Logger.error("Callback failed. HttpResponse was not valid after Post for token." + 
						(sandboxLogin ? "Sandbox login attempt." : "Prod login attempt."));
				Logger.error("Response body from post: %s", response.getString());

				JsonPrimitive error = r.getAsJsonPrimitive("error");
				JsonPrimitive errorDescription = r.getAsJsonPrimitive("error_description");
				if (error != null && errorDescription != null) {
					listener.onFailure(error.getAsString(), errorDescription.getAsString());
				} else if (error != null) {
					listener.onFailure(error.getAsString(), "No error message description.");
				} else if (errorDescription != null) {
					listener.onFailure("Response from oauth2/token invalid.", errorDescription.getAsString());
				} else {
					listener.onFailure("Response from oauth2/token invalid.", "No error message description.");
				}
			}
		}
	}

	private static boolean attributesPresent(JsonObject object, ArrayList<String> attributes) {
		for (String attribute : attributes) {
			if (object.getAsJsonPrimitive(attribute) == null) { return false; }
		}
		return true;	// all attributes found
	}
	
	public static boolean refreshToken(String tokenURL, String clientId,
			String sec) {
		if (!isLoggedIn())
			return false;

		OAuthSession currentSession = getOAuthSession();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("grant_type", "refresh_token");
		params.put("refresh_token", currentSession.refresh_token);
		params.put("client_id", clientId);
		params.put("client_secret", sec);
		HttpResponse response = WS.url(tokenURL).params(params).post();
		JsonObject r = response.getJson().getAsJsonObject();

		// ensure all expected elements are present in response object
		if (attributesPresent(r, Lists.newArrayList(ACCESS_TOKEN,
				ID_ATTR, INSTANCE_URL, SIGNATURE))) {

			currentSession.access_token = r.getAsJsonPrimitive(ACCESS_TOKEN).getAsString();
			currentSession.idURL = r.getAsJsonPrimitive(ID_ATTR).getAsString();
			currentSession.instance_url = r.getAsJsonPrimitive(INSTANCE_URL).getAsString();
			currentSession.signature = r.getAsJsonPrimitive(SIGNATURE).getAsString();

			String id = currentSession.idURL.substring(currentSession.idURL
					.lastIndexOf('/') + 1);
			currentSession.uid = id;

			Cache.set(session.getId() + "-oauth", currentSession);
			if (isPersistentSession()) {
				currentSession.merge();
			}
		}
		return true;
	}

	public static interface OAuthListner extends Serializable {
		public abstract void onSuccess(OAuthSession session);

		public abstract void onFailure(String error, String errorDesc);
	}
}