package models;

import javax.persistence.Column;
import javax.persistence.Entity;

import play.db.jpa.JPABase;
import play.db.jpa.Model;
import play.libs.Crypto;

/*
 * Play-ing in Java - By SANDEEP BHANOT
 * http://blogs.developerforce.com/developer-relations/2011/08/play-ing-in-java.html
 * https://github.com/sbhanot-sfdc/Play-Force
 */

@Entity
public class OAuthSession extends Model {

	public String uid;
	public String idURL;

	@Column(length = 300)
	public String access_token;

	@Column(length = 300)
	public String refresh_token;
	public String instance_url;
	public String signature;

	public OAuthSession() {
	}

	public static OAuthSession get(String uid) {
		OAuthSession sess = find("uid", uid).first();
		if (sess != null) {
			sess.access_token = Crypto.decryptAES(sess.access_token);
			sess.refresh_token = Crypto.decryptAES(sess.refresh_token);
		}
		return sess;
	}

	@Override
	public <T extends JPABase> T save() {
		String before_encrypt_at = access_token;
		String before_encrypt_rt = refresh_token;

		access_token = Crypto.encryptAES(access_token);
		refresh_token = Crypto.encryptAES(refresh_token);

		T ret = super.save();
		access_token = before_encrypt_at;
		refresh_token = before_encrypt_rt;
		return ret;
	}

	@Override
	public <T extends JPABase> T merge() {
		String before_encrypt_at = access_token;
		String before_encrypt_rt = refresh_token;

		access_token = Crypto.encryptAES(access_token);
		refresh_token = Crypto.encryptAES(refresh_token);

		T ret = super.merge();
		access_token = before_encrypt_at;
		refresh_token = before_encrypt_rt;
		return ret;
	}
}