package i5.las2peer.services.iStarMLModelService.mail;



import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Allows sending an e-mail (e.g. to notify an admin)
 * @author Alexander
 *
 */
public class MailSender {

	public static void Send(MailData data) throws Exception
	{
		 Properties properties = System.getProperties();
		 properties.setProperty("mail.smtp.starttls.enable", "true");
		 properties.setProperty("mail.smtp.host", data.getHost());
		 properties.setProperty("mail.smtp.port", Integer.toString(data.getPort()));
		 properties.setProperty("mail.smtp.user",data.getUser());
		 properties.setProperty("mail.smtp.password", data.getPass());
		 
		 properties.setProperty("mail.smtp.auth", "true");
		 Session session = Session.getDefaultInstance(properties, new SmtpAuthenticator(data.getUser(), data.getPass()));
		 
         MimeMessage message = new MimeMessage(session);
        // message.setFrom(new InternetAddress(user));
         message.addRecipient(Message.RecipientType.TO,new InternetAddress(data.getTo()));	        
         message.setSubject(data.getSubject());
         message.setText(data.getText());
         Transport.send(message);	         
	     //data=null;
	}
	
	
}
