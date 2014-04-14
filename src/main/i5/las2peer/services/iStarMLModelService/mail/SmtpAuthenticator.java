package i5.las2peer.services.iStarMLModelService.mail;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * Necessary for smtp server authentication (to send mails)
 * @author Alexander
 *
 */
public class SmtpAuthenticator extends Authenticator {
	private String _user;
	private String _pass;
	/**
	 * Constructor
	 * @param user
	 * @param pass
	 */
	public SmtpAuthenticator(String user, String pass) {

	    super();
	    _user=user;
	    _pass=pass;
	}
	/**
	 * provides login data for the smtp server
	 */
	@Override	
	public PasswordAuthentication getPasswordAuthentication() {
	
	    if ((_user != null) && (!_user.isEmpty()) && (_pass != null) 
	      && (!_pass.isEmpty())) {

	        return new PasswordAuthentication(_user, _pass);
	    }

	    return null;
	}
}
