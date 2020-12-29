package com.rak.aws.sessamples;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
// AWS SDK libraries. Download the AWS SDK for Java 
// from https://aws.amazon.com/sdk-for-java
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.GetTemplateRequest;
import com.amazonaws.services.simpleemail.model.GetTemplateResult;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

import de.agitos.dkim.Canonicalization;
import de.agitos.dkim.DKIMSigner;
import de.agitos.dkim.SMTPDKIMMessage;
import de.agitos.dkim.SigningAlgorithm;

public class AmazonSESDKIMSIGN {

	//TODO change the below variables according to your environment
	private static String SENDER = "noreply@skiply.ae";

	private static String RECIPIENT = "rakmobility@gmail.com";

	private static String DOMAIN = "qa.skiply.ae";

	private static String KEY_SELECTOR = "dkim2048";

	private static String PRIVATE_KEY_PATH = "/Users/syam/projects/skiply/dkim2048/uat/private.key.der";

	private static String ACCESS_KEY_ID = "";
	private static String SECRET_KEY = "";
	
	private static String SES_TEMPLATE_NAME = "MyTemplate";

	public static void main(String[] args) throws AddressException, MessagingException, IOException,
			InvalidKeySpecException, NoSuchAlgorithmException, Exception {

		Session session = Session.getDefaultInstance(new Properties());
		session.setDebug(true);

		BasicAWSCredentials auth = new BasicAWSCredentials(ACCESS_KEY_ID, SECRET_KEY);
		AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(auth)).withRegion(Regions.EU_WEST_1).build();

		// Sign Email with DKIM
		DKIMSigner des = null;
		des = new DKIMSigner(DOMAIN, KEY_SELECTOR, PRIVATE_KEY_PATH);
		des.setHeaderCanonicalization(Canonicalization.SIMPLE);
		des.setBodyCanonicalization(Canonicalization.RELAXED);
		des.setLengthParam(true);
		des.setSigningAlgorithm(SigningAlgorithm.SHA256withRSA);
		des.setZParam(true);

		// Create a new MimeMessage object.
		MimeMessage message = new MimeMessage(session);
		MimeBodyPart textPart = new MimeBodyPart();
		MimeBodyPart htmlPart = new MimeBodyPart();

		// Add subject, from and to lines.
		message.setFrom(new InternetAddress(SENDER));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(RECIPIENT));

		// Create a multipart/alternative child container.
		MimeMultipart msg_body = new MimeMultipart("alternative");

		// Create a wrapper for the HTML and text parts.
		MimeBodyPart wrap = new MimeBodyPart();

		// Add the child container to the wrapper object.
		wrap.setContent(msg_body);

		// Create a multipart/mixed parent container.
		MimeMultipart msg = new MimeMultipart("mixed");

		// Add the parent container to the message.
		message.setContent(msg);

		// Add the multipart/alternative part to the message.
		msg.addBodyPart(wrap);

		// Define the attachment
		MimeBodyPart att = new MimeBodyPart();

		// Try to send the email.
		try {
			System.out.println("Attempting to send an email through Amazon SES " + "using the AWS SDK for Java...");

			GetTemplateRequest getTemplateRequest = new GetTemplateRequest();
			getTemplateRequest.setTemplateName(SES_TEMPLATE_NAME);
			GetTemplateResult getTemplateResult = client.getTemplate(getTemplateRequest);
			String templatesubject = getTemplateResult.getTemplate().getSubjectPart();

			message.setSubject(templatesubject, "UTF-8");
			message.setFrom(new InternetAddress(SENDER));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(RECIPIENT));

			String templateBodyTxt = getTemplateResult.getTemplate().getTextPart();
			textPart.setContent(templateBodyTxt, "text/plain; charset=UTF-8");

			String templateBodyhtml = getTemplateResult.getTemplate().getHtmlPart();
			htmlPart.setContent(templateBodyhtml, "text/html; charset=UTF-8");

			msg_body.addBodyPart(textPart);
			msg_body.addBodyPart(htmlPart);

			wrap.setContent(msg_body);

			PrintStream out = System.out;
			message.writeTo(out);

			// Send the email.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			SMTPDKIMMessage dkimessage = new SMTPDKIMMessage(message, des);
			// message.writeTo(outputStream);
			dkimessage.writeTo(outputStream);
			RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

			SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage)
			// .withConfigurationSetName(CONFIGURATION_SET)
			;

			client.sendRawEmail(rawEmailRequest);
			System.out.println("Email sent!");

		} catch (Exception ex) {
			System.out.println("Email Failed");
			System.err.println("Error message: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}