package harvester.processor.util;
import org.dom4j.*;
import org.dom4j.io.*;
import java.io.*;
import java.util.*;
import java.text.*;
import org.apache.commons.lang.*;

/** 
 * Mapping between DC and EAC.
 * @author Tor Lattimore
 */
public class DCToEAC {
	String xml;
	Document doc;

	public void initialize(Document doc) {
		xml = "";
		this.doc = doc;
	}

	/** Convert and return the EAC record as a string.
	 * @return A EAC record as a string.
	 */
	public String convert() {
		write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");		
		write("<eac type=\"persname\" xmlns=\"http://jefferson.village.virginia.edu/eac\">");
		writeHeader();
		writeCondesc();
		write("</eac>");
		return xml;
	}

	void write(String tag) {
		xml+=tag + "\n";
	}

	void write_esc(String data) {
		xml += StringEscapeUtils.escapeXml(data);
	}

	String esc(String data) {
		return StringEscapeUtils.escapeXml(data);
	}


	void writeHeader() {
		write("<eacheader detaillevel=\"full\" status=\"edited\">");
		write("<eacid>");
		Node node = doc.selectSingleNode("/record/metadata/dc/identifier");
		write_esc(node.getText());
		write("</eacid>");
		writeMainHist();
		writeLanguageDecl();
		writeSourceDecl();
		write("</eacheader>");
	}
	void writeSourceDecl() {
		write("<sourcedecl>");
		List nodes = doc.selectNodes("/record/metadata/dc/identifier");
		for (Object node : nodes) {
			write("<source syskey=\"" + esc(((Node)node).getText()) + "\">");
			write("</source>");
		}
		
		Node node = doc.selectSingleNode("/record/header/identifier");
		write("<source ownercode=\"oaiid\" syskey=\"" + esc(node.getText()) + "\">"); 
		write_esc(node.getText());
		write("</source>");
		write("</sourcedecl>");
	}

	void writeLanguageDecl() {
		write("<languagedecl>");
		List nodes = doc.selectNodes("/record/metadata/dc/language");
		for (Object node : nodes) {
			String text = ((Node)node).getText();
			write("<language>" + esc(text) + "</language>");
		}
		write("</languagedecl>");
	}

	void writeMainHist() {
		String normal = (new SimpleDateFormat("yyyyMMdd")).format(new Date());
			
		write("<mainhist>");
		write("<mainevent maintype=\"create\">");
		write("<maindate calendar=\"gregorian\" normal=\"" + normal + "\">");
		write(normal);
		write("</maindate>");
		write("<maindesc>Converted to DC by NLAHarvester</maindesc>");
		write("<name>NLAHarvester</name>");
		write("</mainevent>");
		write("</mainhist>");
	}

	void writeCondesc() {
		write("<condesc>");
		writeIdentity();
		List nodes = doc.selectNodes("/record/metadata/dc/description");
		for (Object node : nodes) {
			String text = ((Node)node).getText();
			write("<desc>");
			write("<persdesc>");
			write("<descentry>");
			write("<value>");
			write_esc(text);
			write("</value>");
			write("</descentry>");
			write("</persdesc>");
			write("</desc>");
		}
		writeResourceRel();
		write("</condesc>");
	}

	void writeIdentity() {
		write("<identity>");
		List nodes = doc.selectNodes("/record/metadata/dc/subject");
		for (Object node : nodes) {
			String text = ((Node)node).getText();
			write("<pershead>");
			write("<part>");
			write_esc(text);
			write("</part>");
			write("</pershead>");
		}
		write("</identity>");
	}

	void writeResourceRel() {
		/* 
		 * creator -> name
		 * contributor -> name
		 * publisher -> imprint/publisher
		 * date -> imprint/date
		 * format -> descnote/genreform
		 * title -> title
		 * type -> descnote/genreform
		*/
		ArrayList creators = new ArrayList(doc.selectNodes("/record/metadata/dc/creator"));
		ArrayList contributors = new ArrayList(doc.selectNodes("/record/metadata/dc/contributor"));
		ArrayList publishers = new ArrayList(doc.selectNodes("/record/metadata/dc/publisher"));
		ArrayList dates = new ArrayList(doc.selectNodes("/record/metadata/dc/date"));
		ArrayList formats = new ArrayList(doc.selectNodes("/record/metadata/dc/format"));
		ArrayList titles = new ArrayList(doc.selectNodes("/record/metadata/dc/title"));
		ArrayList types = new ArrayList(doc.selectNodes("/record/metadata/dc/type"));


		creators.addAll(contributors);
		formats.addAll(types);
		publishers.addAll(dates);

		int[] counts = {creators.size(), publishers.size(), formats.size(), titles.size()};
		int bibunit_size = counts[0];
		int i;

		for (int count : counts) {
			if (count > bibunit_size) {
				bibunit_size = count;
			}
		}

		write("<resourcerels>");
		write("<resourcerel>");
		write("<bibunit>");
		for (i = 0;i < bibunit_size;i++) {
			write("<name>");
			if (i < creators.size()) 	write_esc(((Node)creators.get(i)).getText());	
			write("</name>");

			write("<title>");
			if (i < titles.size()) 		write_esc(((Node)titles.get(i)).getText());
			write("</title>");

			write("<edition/>");

			write("<imprint>");

			if (i < publishers.size() - dates.size()) write("<publisher>");
			else write("<date>");
			if (i < publishers.size())	write_esc(((Node)publishers.get(i)).getText());
			if (i < publishers.size() - dates.size()) write("</publisher>");
			else write("</date>");

			write("</imprint>");

			write("<bibseries/>");

			write("<descnote>");
			write("<genreform>");
			if (i < formats.size())		write_esc(((Node)formats.get(i)).getText());
			write("</genreform>");
			write("</descnote>");
		}
		write("</bibunit>");
		write("</resourcerel>");
		write("</resourcerels>");
	}
}

















