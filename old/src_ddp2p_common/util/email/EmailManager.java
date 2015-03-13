/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2013 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */

package util.email;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

import util.ByteDataSource;
import util.DBInterface;
import util.DD_EmailableAttachment;
import util.DD_IdentityVerification_Request;
import util.P2PDDSQLException;
import util.Util;

import static util.Util.__;
import config.Application;
import config.Application_GUI;
import config.DD;
import data.D_Constituent;
import data.D_Peer;
import data.HandlingMyself_Peer;

public 
class EmailManager {
	public static final String SMTP_PASSWORD = "SMTP_PASSWORD";
	private static final String SMTP_USERNAME = "SMTP_USERNAME";
	private static final String SMTP_HOST = "SMTP_HOST";
	static boolean DEBUG = false;
		
	public static void main(String args[]) {
		try {
			if(args.length == 0) {
				System.out.println("prog database id fix verbose");
				return;
			}
			
			String database = Application.DELIBERATION_FILE;
			if(args.length>0) database = args[0];
			Application.db = new DBInterface(database);

			String password = null;
			String host = null;
			if(args.length>1) host = args[1];
			if(args.length>2) password = args[2];

			DD_IdentityVerification_Request iv = new DD_IdentityVerification_Request();
			iv.d = Util.getGeneralizedTime();
			iv.r = new BigInteger(300, new SecureRandom());
			iv.verified = D_Constituent.getConstByLID(new Long(2), true, false);
			iv.verifier = D_Peer.getPeerByLID_NoKeep(1, true);
			

			if(host == null) host = "smtp-server.cfl.rr.com";
			String user = "msilaghi";
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}

	static void sendEmail(DD_EmailableAttachment iv, 
			String text_greeting, String attachment_filename,
			String host, String username, String password, 
			String from, String to, String subject
			)
	{

        // Create properties for the Session
        Properties props = new Properties();
 
        // If using static Transport.send(),
        // need to specify the mail server here
        props.put("mail.smtp.host", host);
        // To see what is going on behind the scene
        props.put("mail.debug", DEBUG+"");
 
        // Get a session
        Session session = Session.getInstance(props);

        try {
            // Get a Transport object to send e-mail
            Transport bus = session.getTransport("smtp");
 
            if(password==null) bus.connect();
            else bus.connect(host, username, password);
 

            // Instantiate a message
            Message msg = new MimeMessage(session);
 
            // Set message attributes
            msg.setFrom(new InternetAddress(from));
            InternetAddress[] address = {new InternetAddress(to)};
            msg.setRecipients(Message.RecipientType.TO, address);
            // Parse a comma-separated list of email addresses. Be strict.
            msg.setRecipients(Message.RecipientType.CC,
                                InternetAddress.parse(to, true));
            // Parse comma/space-separated list. Cut some slack.
            msg.setRecipients(Message.RecipientType.BCC,
                                InternetAddress.parse(to, false));
 
            msg.setSubject(subject);
            msg.setSentDate(new Date());
 
            // Set message content and send
            setContent(msg, text_greeting, iv, attachment_filename);
            msg.saveChanges();
            bus.sendMessage(msg, address);
	}
        catch (MessagingException mex) {
            // Prints all nested (chained) exceptions as well
            mex.printStackTrace();
            // How to access nested exceptions
            while (mex.getNextException() != null) {
                // Get next exception in chain
                Exception ex = mex.getNextException();
                ex.printStackTrace();
                if (!(ex instanceof MessagingException)) break;
                else mex = (MessagingException)ex;
            }
        }
	}
	private static void setContent(Message msg, String text_greeting, DD_EmailableAttachment iv,
			String attachment_filename) throws MessagingException {
        // Create and fill first part
        MimeBodyPart p1 = new MimeBodyPart();
        p1.setText(text_greeting, "utf8"); //"us-ascii"
 
        // Create and fill second part
        byte[] data = iv.get_ByteContent();
        //System.out.println("EmailManager: setContent: Message length="+data.length);
        //InternetHeaders headers = new InternetHeaders();
		//MimeBodyPart p2 = new MimeBodyPart(headers, data);//needs data in base64
		MimeBodyPart p2 = new MimeBodyPart();

        // Put a file in the second part
        ByteDataSource bds = new ByteDataSource(data, attachment_filename);
        p2.setDataHandler(new DataHandler(bds));
        p2.setFileName(attachment_filename);

        // Create the Multipart.  Add BodyParts to it.
        Multipart mp = new MimeMultipart();
        mp.addBodyPart(p1);
        mp.addBodyPart(p2);
 
        // Set Multipart as the message's content
        msg.setContent(mp);
	}

	public static void sendEmail(DD_EmailableAttachment iv) {
		String host = null;
		String user = null;
		String password = null;
		try {
			host = DD.getAppText(SMTP_HOST);
			user = DD.getAppText(SMTP_USERNAME);
			password = DD.getAppText(SMTP_PASSWORD);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		try{
			if(host==null) {
				host = Application_GUI.input(__("Enter the SMTP Host"), __("Email host"),
						Application_GUI.QUESTION_MESSAGE);
				DD.setAppText(SMTP_HOST, host);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		try{
			if(user==null) {
				user = Application_GUI.input(__("Email username:"+" "+host), __("Email username"),
						Application_GUI.QUESTION_MESSAGE);
				DD.setAppText(SMTP_USERNAME, user);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		try{
			if(password==null) {
				password = Application_GUI.input(__("Password for:"+" "+user+" at "+host), __("Email password"),
						Application_GUI.QUESTION_MESSAGE);
				DD.setAppText(SMTP_PASSWORD, password);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		
		
		String filename = iv.get_FileName();
		String to_email = iv.get_To();
		if(to_email == null){
			Application_GUI.warning(__("Abandon as I do not know where to send the message!"), __("Verification abandoned"));
			return;
		}
		String from = iv.get_From();
		String subject = iv.get_Subject();
		sendEmail(iv, iv.get_Subject(),
				filename, 
				host, user, password,
				from, to_email,
				subject
				);
	}

	public static void verify(long constituent_ID) {
			DD_IdentityVerification_Request iv = new DD_IdentityVerification_Request();
			iv.verified = D_Constituent.getConstByLID(constituent_ID, true, false); // new D_Constituent(constituent_ID);
			iv.verifier = HandlingMyself_Peer.get_myself_with_wait();
			iv.d = Util.getGeneralizedTime();
			iv.r = new BigInteger(DD_IdentityVerification_Request.R_SIZE, new SecureRandom());
			//DD.setAppText("VI:"+iv.verified.global_constituent_id_hash, iv.r.toString());
			table.constituent_verification.add(constituent_ID, iv.r.toString(),iv.d);

			sendEmail(iv);
	}

	public static void setEmailPassword(Object current_id_branch, String object) {
		try {
			DD.setAppTextNoSync(util.email.EmailManager.SMTP_PASSWORD, object);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}

	public static void setEmailUsername(Object current_id_branch, String object) {
		try {
			DD.setAppTextNoSync(util.email.EmailManager.SMTP_USERNAME, object);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}

	public static void setSMTPHost(Object current_id_branch, String object) {
		try {
			DD.setAppTextNoSync(util.email.EmailManager.SMTP_HOST, object);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}

	public static String getSMTPHost(Object current_id_branch) {
		try {
			return DD.getExactAppText(util.email.EmailManager.SMTP_HOST);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getEmailUsername(Object current_id_branch) {
		try {
			return DD.getExactAppText(util.email.EmailManager.SMTP_USERNAME);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
	}
}
