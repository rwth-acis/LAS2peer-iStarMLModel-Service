package i5.las2peer.services.iStarMLModelService.mail;

/**
 * Stores data necessary to send a mail for notification
 * @author Alexander
 *
 */
public class MailData {
	String _to;
	String _host;
	int _port;
	String _user;
	String _pass;
	String _subject;
	String _text;
	
	public String getTo() {
		return _to;
	}

	public String getHost() {
		return _host;
	}

	public int getPort() {
		return _port;
	}

	public String getUser() {
		return _user;
	}

	public String getPass() {
		return _pass;
	}

	public String getSubject() {
		return _subject;
	}

	public String getText() {
		return _text;
	}
	
	public void setText(String text)
	{
		_text=text;
	}
	public void setSubject(String subject)
	{
		_subject=subject;
	}
	public MailData(String to, String host,int port, String user, String pass, String subject, String text)
	{
		_to=to;
		_host=host;
		_port=port;
		_user=user;
		_pass=pass;
		_subject=subject;
		_text=text;
	}
	
}
