package harvester.processor.email;


import harvester.data.*;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.ServletContext;
import org.apache.velocity.app.*;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.*;

/** handles the logic for sending email, including selecting the right velocity template to use and filling in the template */
public class Email {

	private static Logger logger = Logger.getLogger(Email.class);
	
	protected Contributor c;
	protected Harvest h;
	protected Properties props;
	protected ServletContext ctx;
	protected VelocityEngine ve;
	protected VelocityContext colcontext;
	protected VelocityContext concontext;
	
	private String harvesterrors = "harvesterrors.vm";
	private String recordfailure = "recordfailure.vm";
	private String harvestfailure = "harvestfailure.vm";
	private String success = "success.vm";
	
	public static int SUCCESS = 0;
	public static int RECORD_FAILURES = 1;
	public static int HARVEST_FAILURE = 2;
	public static int HARVEST_ERRORS = 3;
	
	private interface FilterContributor {
		public boolean acceptConContact(ContributorContact contact);
		public boolean acceptColContact(ContactSelections selection);
	}
	
	
	public void EmailInit() throws Exception
	{

		String templatefolder = props.getProperty("mail.templatefolder");
		
		//configure the velocity engine
        ve = new VelocityEngine();
        ve.addProperty("file.resource.loader.path", ctx.getRealPath(templatefolder));
        ve.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.Log4JLogChute" ); 
        ve.setProperty("runtime.log.logsystem.log4j.logger", "org.apache.velocity");
        
        ve.init();
        LogFormatter lf = new LogFormatter(h.getHarvestid());
        Map<String, Object> map = lf.generateEmailReport();
        
        map.put("formatter", lf);
        map.put("type", h.getType() == Profile.TEST_PROFILE ? "test" : "production" );
        map.put("contributor", c);
        map.put("serverName", props.get("mail.viewLogServerName"));
        map.put("viewlogurl", props.get("mail.viewlogurl") + String.valueOf(h.getHarvestid()));
        
        colcontext = new VelocityContext();
        for(String key : map.keySet()) {
        	colcontext.put(key, map.get(key));
        }
        colcontext.put("internal", true);

        concontext = new VelocityContext();
        for(String key : map.keySet()) {
        	concontext.put(key, map.get(key));
        }
        
        
	}
	
	private void emailForType(String msgfile, FilterContributor filter, String subject) throws Exception {
		logger.info("emailing on successful contacts");
		
        Template t = ve.getTemplate(msgfile, "UTF-8");
        StringWriter conwriter = new StringWriter();
        t.merge( concontext, conwriter );
        String conmsg = conwriter.toString();

        StringWriter colwriter = new StringWriter();
        t.merge( colcontext, colwriter );
        String colmsg = colwriter.toString();
        
        logger.debug(conmsg);
		
		//create a new set with a only contact on successful contacts with existing emails
		for(ContributorContact contact : c.getContacts()) {
			if(contact.getEmail() != null && !contact.getEmail().equals("") && filter.acceptConContact(contact)) {

				List<String> contactList = new LinkedList<String>(); 
				contactList.add(contact.getEmail());
				email(contactList, conmsg, subject);
			}
		}
		for(ContactSelections sel : c.getContactselections()) {
			if(sel.getContact().getEmail() != null && !sel.getContact().getEmail().equals("") && filter.acceptColContact(sel)) {
				
				List<String> contactList = new LinkedList<String>(); 
				contactList.add(sel.getContact().getEmail());
				email(contactList, colmsg, subject);
			}
		}

	}
	
	public void emailSuccess() throws Exception {
		emailForType(success, new FilterContributor() {
				public boolean acceptConContact(ContributorContact contact) { return contact.getSuccess() == 1;}
				public boolean acceptColContact(ContactSelections selection) { return selection.getSuccess() == 1;}
			}, 
			c.getCollection().getName() + " harvest for " + c.getName() + " was successful");
	}
	
	public void emailRecordFailure() throws Exception {
		emailForType(recordfailure, new FilterContributor() {
				public boolean acceptConContact(ContributorContact contact) { return contact.getRecord() == 1;}
				public boolean acceptColContact(ContactSelections selection) { return selection.getRecord() == 1;}
			}, 
			"one or more records failed when harvesting from " + c.getCollection().getName() + " for " + c.getName());
	}
	
	public void emailHarvestFailure() throws Exception {
		emailForType(harvestfailure, new FilterContributor() {
				public boolean acceptConContact(ContributorContact contact) { return contact.getFailure() == 1;}
				public boolean acceptColContact(ContactSelections selection) { return selection.getFailure() == 1;}
			}, 
			"Harvest failed harvesting from " + c.getCollection().getName() + " for " + c.getName());
	}
	
	public void emailHarvestErrors() throws Exception {
		emailForType(harvesterrors, new FilterContributor() {
				public boolean acceptConContact(ContributorContact contact) { return contact.getHarvest() == 1;}
				public boolean acceptColContact(ContactSelections selection) { return selection.getHarvest() == 1;}
			}, 
			"one or more records failed when harvesting from " + c.getCollection().getName() + " for " + c.getName());
	}
	
//	public void emailSuccess() throws Exception
//	{
//
//		logger.info("emailing on successful contacts");
//		//create a new set with a only contact on successful contacts with existing emails
//		List<String> filteredcontacts = new LinkedList<String>(); 
//		for(ContributorContact contact : c.getContacts())
//			if(contact.getEmail() != null && !contact.getEmail().equals("") && contact.getSuccess() == 1)
//				filteredcontacts.add(contact.getEmail());
//		for(ContactSelections sel : c.getContactselections())
//			if(sel.getContact().getEmail() != null && !sel.getContact().getEmail().equals("") && sel.getSuccess() == 1)
//				filteredcontacts.add(sel.getContact().getEmail());
//		
//		//run the velocity engine over the success template
//
//        Template t = ve.getTemplate(success, "UTF-8");
//        
//        //now render the template into a StringWriter
//        StringWriter writer = new StringWriter();
//        t.merge( colcontext, writer );
//		
//        String subject = c.getCollection().getName() + " harvest for " + c.getName() + " was successful";
//        
//		email(filteredcontacts, writer.toString(), subject);
//		
//	}
	
	
//	public void emailRecordFailure() throws Exception
//	{
//		logger.info("emailing to failed record contacts");
//		//create a new set with a only contact on successful contacts with existing emails
//		List<String> filteredcontacts = new LinkedList<String>(); 
//		for(ContributorContact contact : c.getContacts())
//			if(contact.getEmail() != null && !contact.getEmail().equals("") && contact.getRecord()== 1)
//				filteredcontacts.add(contact.getEmail());
//		for(ContactSelections sel : c.getContactselections())
//			if(sel.getContact().getEmail() != null && !sel.getContact().getEmail().equals("") && sel.getRecord() == 1)
//				filteredcontacts.add(sel.getContact().getEmail());
//		//run the velocity engine over the success template
//
//        Template t = ve.getTemplate(recordfailure, "UTF-8");
//        
//        //now render the template into a StringWriter
//        StringWriter writer = new StringWriter();
//        t.merge( colcontext, writer );
//		
//        String subject = "one or more records failed when harvesting from " + c.getCollection().getName() + " for " + c.getName();
//        
//		email(filteredcontacts, writer.toString(), subject);
//	}
	
//	public void emailHarvestFailure() throws Exception
//	{
//		logger.info("emailing to harvest failed contacts");
//		//create a new set with a only contact on successful contacts with existing emails
//		List<String> filteredcontacts = new LinkedList<String>(); 
//		for(ContributorContact contact : c.getContacts())
//			if(contact.getEmail() != null && !contact.getEmail().equals("") && contact.getFailure() == 1)
//				filteredcontacts.add(contact.getEmail());
//		for(ContactSelections sel : c.getContactselections())
//			if(sel.getContact().getEmail() != null && !sel.getContact().getEmail().equals("") && sel.getFailure() == 1)
//				filteredcontacts.add(sel.getContact().getEmail());
//		
//		//run the velocity engine over the success template
//
//        Template t = ve.getTemplate(harvestfailure, "UTF-8");
//        
//        //now render the template into a StringWriter
//        StringWriter writer = new StringWriter();
//        t.merge( colcontext, writer );
//		
//        String subject = "Harvest failed harvesting from " + c.getCollection().getName() + " for " + c.getName();
//        
//		email(filteredcontacts, writer.toString(), subject);
//	}
//	public void emailHarvestErrors() throws Exception
//	{
//		logger.info("emailing on harvest error contacts");
//		//create a new set with a only contact on successful contacts with existing emails
//		List<String> filteredcontacts = new LinkedList<String>(); 
//		for(ContributorContact contact : c.getContacts())
//			if(contact.getEmail() != null && !contact.getEmail().equals("") && contact.getHarvest() == 1)
//				filteredcontacts.add(contact.getEmail());
//		for(ContactSelections sel : c.getContactselections())
//			if(sel.getContact().getEmail() != null && !sel.getContact().getEmail().equals("") && sel.getHarvest() == 1)
//				filteredcontacts.add(sel.getContact().getEmail());
//		
//		//run the velocity engine over the success template
//
//        Template t = ve.getTemplate(harvesterrors, "UTF-8");
//        
//        //now render the template into a StringWriter
//        StringWriter writer = new StringWriter();
//        t.merge( colcontext, writer );
//		
//        String subject = "one or more records failed when harvesting from " + c.getCollection().getName() + " for " + c.getName();
//        
//		email(filteredcontacts, writer.toString(), subject);
//	}
	
	/**
	 * emails the passed contributor contacts
	 * @param contacts a set of contacts to email
	 * @param emailtext the text of the email
	 * @param subject the subject of the email
	 * @throws Exception
	 */
	private void email(List<String> contacts, String emailtext, String subject) throws Exception
	{
		if(contacts.size() == 0)
			return;
		
	     boolean debug = false;
	     String from = props.getProperty("mail.from");
	     logger.info("from=" + from);

	    // create some properties and get the default Session
	    Session session = Session.getDefaultInstance(props, null);
	    session.setDebug(debug);

	    // create a message
	    Message msg = new MimeMessage(session);

	    // set the from and to address
	    InternetAddress addressFrom = new InternetAddress(from);
	    msg.setFrom(addressFrom);

	    InternetAddress[] addressTo = new InternetAddress[contacts.size()];
	    int i = 0;
	    for(String contact : contacts)
	    {
	        addressTo[i] = new InternetAddress(contact);
	        i++;
	        logger.info("emailing: " + contact);
	    }
	    msg.setRecipients(Message.RecipientType.TO, addressTo);

	    // Setting the Subject and Content Type
	    msg.setSubject(subject);
	    msg.setContent(emailtext, "text/html");
	    Transport.send(msg);
	    
	}
	

	public Contributor getC() {
		return c;
	}
	public void setC(Contributor c) {
		this.c = c;
	}
	public Harvest getH() {
		return h;
	}
	public void setH(Harvest h) {
		this.h = h;
	}
	public Properties getProps() {
		return props;
	}
	public void setProps(Properties props) {
		this.props = props;
	}
	public ServletContext getCtx() {
		return ctx;
	}
	public void setCtx(ServletContext ctx) {
		this.ctx = ctx;
	}
	
}
