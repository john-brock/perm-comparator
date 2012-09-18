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
import com.google.gson.JsonObject;
import play.libs.WS.HttpResponse;
import java.io.Serializable;

/*
 * Play-ing in Java - By SANDEEP BHANOT
 * http://blogs.developerforce.com/developer-relations/2011/08/play-ing-in-java.html
 * https://github.com/sbhanot-sfdc/Play-Force
 */
public class ForceDotComOAuth2 extends Controller {

	private static String authorizationURL;
	private static String accessTokenURL;
	private static String clientid;
	private static String secret;

	public static boolean isLoggedIn() {
		return (getOAuthSession() != null) ? true : false;
	}

	public static OAuthSession getOAuthSession() {
		OAuthSession sess = (OAuthSession) Cache
				.get(session.getId() + "-oauth");
		Logger.info("Session in Cache is:" + sess);
		return sess;
	}

	public static boolean login(String clientId, String sec, OAuthListner list) {
		return login("https://login.salesforce.com/services/oauth2/authorize",
				"https://login.salesforce.com/services/oauth2/token", clientId,
				sec, list);
	}

	public static boolean login(String authURL, String tokenURL,
			String clientId, String sec, OAuthListner list) {
		if (list == null)
			return false;

		authorizationURL = authURL;
		accessTokenURL = tokenURL;
		clientid = clientId;
		secret = sec;

		OAuthSession sess = (OAuthSession) Cache
				.get(session.getId() + "-oauth");
		Logger.info("sess in login is:" + sess);
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
						Logger.info("persistent sess in login is:" + sess);
						if (sess == null) {
							response.removeCookie("uid");
							initiateOAuthFlow(list);
						}
					} else {
						response.removeCookie("uid");
						initiateOAuthFlow(list);
					}
				} else {
					initiateOAuthFlow(list);
				}
			} else {
				initiateOAuthFlow(list);
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
		if (sess != null) {
			Cache.safeDelete(session.getId() + "-oauth");
			Logger.info("cache removed in logout:");
			OAuthSession u = OAuthSession.get(sess.uid);
			Logger.info("persistent session in logout:" + u);
			if (u != null)
				u.delete();
		}
		return true;
	}

	/*
	 * public User getUser() { }
	 */

	private static boolean isPersistentSession() {
		String persistent = Play.configuration
				.getProperty("sfdc.persistentSession");
		return (persistent != null && persistent.equals("true")) ? true : false;
	}

	private static void initiateOAuthFlow(OAuthListner list) {
		Logger.info("initiateOAuthFlow called");
		Cache.set(session.getId() + "-listener", list);
		throw new Redirect(authorizationURL
				+ "?response_type=code&client_id="
				+ clientid
				+ "&redirect_uri="
				+ WS.encode(play.mvc.Router
						.getFullUrl("ForceDotComOAuth2.callback")));
	}

	public static void callback() {
		Logger.info("callback called");
		OAuthListner listener = (OAuthListner) Cache.get(session.getId()
				+ "-listener");
		if (listener == null)
			return;

		if (!Cache.safeDelete(session.getId() + "-listener")) {
			Logger.error("Could not remove from cache");
			return;
		}

		String accessCode = Params.current().get("code");

		if (accessCode == null) {
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
			HttpResponse response = WS.url(accessTokenURL).params(params)
					.post();
			JsonObject r = response.getJson().getAsJsonObject();

			String accessToken = r.getAsJsonPrimitive("access_token")
					.getAsString();
			if (accessToken != null) {
				OAuthSession s = new OAuthSession();
				s.access_token = accessToken;
				s.refresh_token = r.getAsJsonPrimitive("refresh_token")
						.getAsString();
				s.idURL = r.getAsJsonPrimitive("id").getAsString();
				s.instance_url = r.getAsJsonPrimitive("instance_url")
						.getAsString();
				s.signature = r.getAsJsonPrimitive("signature").getAsString();

				String id = s.idURL.substring(s.idURL.lastIndexOf('/') + 1);
				s.uid = id;

				Cache.set(session.getId() + "-oauth", s);
				if (isPersistentSession()) {
					Response.current().setCookie("uid",
							id + "-" + Crypto.sign(id), "30d");
					s.save();
				}
				listener.onSuccess(s);
			} else {
				listener.onFailure(r.getAsJsonPrimitive("error").getAsString(),
						r.getAsJsonPrimitive("error_description").getAsString());
			}
		}
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
		Logger.info("Refresh Token callout complete:" + r);

		String accessToken = r.getAsJsonPrimitive("access_token").getAsString();
		if (accessToken != null) {
			currentSession.access_token = accessToken;
			currentSession.idURL = r.getAsJsonPrimitive("id").getAsString();
			currentSession.instance_url = r.getAsJsonPrimitive("instance_url")
					.getAsString();
			currentSession.signature = r.getAsJsonPrimitive("signature")
					.getAsString();

			String id = currentSession.idURL.substring(currentSession.idURL
					.lastIndexOf('/') + 1);
			currentSession.uid = id;

			Cache.set(session.getId() + "-oauth", currentSession);
			if (isPersistentSession()) {
				currentSession.merge();
			}
		}
		Logger.info("Done");
		return true;
	}

	public static interface OAuthListner extends Serializable {
		public abstract void onSuccess(OAuthSession session);

		public abstract void onFailure(String error, String errorDesc);
	}
}