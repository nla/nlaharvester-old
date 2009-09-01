package harvester.processor.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.marc4j.MarcXmlReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.xml.sax.InputSource;


public class MarcConverter {

    private Record record;

    private boolean valid = true;

    private enum Type { persname, famname, corpname };

    private Type type;

    private String authorized;

    private String existDate = null;

    public MarcConverter() {}

    public void initialize(String marcxml) {
        InputSource is = new InputSource(new StringReader(marcxml));
        MarcXmlReader reader = new MarcXmlReader(is);
        while (reader.hasNext()) {
            record = reader.next();

            DataField tag1XX = getFirstDataField("1[0-1]0");
            if (tag1XX == null) {
                valid = false;
                continue;
            }
            
            // determine if the record contains a subfield t and thus out of scope
            if (tag1XX.getSubfield('t') != null) {
            	valid = false;
            	continue;
            }

            // determine the type of record
            type = Type.persname;
            if (tag1XX.getTag().equals("100") && tag1XX.getIndicator1() == '3') {
                type = Type.famname;
            } else if (tag1XX.getTag().equals("110")) {
                type = Type.corpname;
            }

            // determine the authority for this record
            ControlField tag003 = getFirstControlField("003");
            if (tag003 != null) {
                authorized = tag003.getData();
            } else {
                authorized = "AuCNLKIN";
            }
        }
    }

    /**
     * Check that the type of record is correct.
     * @return
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Convert the record into EAC.
     * @return
     */
    public String convert() {
        StringWriter sw = new StringWriter();
        try {
            XMLStreamWriter w = XMLOutputFactory.newInstance().createXMLStreamWriter(sw);
            w.writeStartDocument("UTF-8", "1.0");
            w.writeCharacters("\n");
            writeEAC(w);
            w.writeEndDocument();
            w.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    public void writeEAC(XMLStreamWriter w) throws Exception {
        char recordType = record.getLeader().getTypeOfRecord();
        if (recordType != 'z') {
            throw new Exception("Not an authority record: " + recordType);
        }

        w.writeStartElement("eac");
        w.writeDefaultNamespace("http://jefferson.village.virginia.edu/eac");
        w.writeAttribute("type", type.toString());
        w.writeCharacters("\n");


        writeHeader(w);
        writeCondesc(w);

        w.writeEndElement();
    }

    public void writeHeader(XMLStreamWriter w) throws Exception {
        w.writeStartElement("eacheader");
        String status = null;
        switch(record.getLeader().getRecordStatus()) {
        case 'n':
            status = "draft";
            break;
        case 'a':
        case 'c': {
            if (record.getLeader().getImplDefined2()[0] == 'o') {
                status = "draft";
            } else {
                status = "edited";
            }
            break;
        }
        case 'd':
        case 's':
        case 'x':
            status = "deleted";
            break;
        }
        w.writeAttribute("status", status);

        String detaillevel = null;
        char detail = getFirstControlField("008").getData().charAt(33);
        switch(detail) {
        case 'a':
            detaillevel = "full";
            break;
        case 'b':
            detaillevel = "partial";
            break;
        case 'c':
            detaillevel = "partial";
            break;
        case 'd':
            detaillevel = "minimal";
            break;
        }
        if (detaillevel != null) {
            w.writeAttribute("detaillevel", detaillevel);
        }

        w.writeCharacters("\n");

        writeEacId(w);
        writeMainHist(w);
        writeLangDecl(w);
        writeRuleDecl(w);
        writeSourceDecl(w);

        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writeEacId(XMLStreamWriter w) throws Exception {
        w.writeStartElement("eacid");
        w.writeAttribute("ownercode", authorized);

        String id = getFirstControlField("001").getData().replaceFirst("^0*", "");
        w.writeAttribute("syskey", id);
        if ("AuCNLKIN".equals(authorized)) {
        	w.writeCharacters("http://nla.gov.au/anbd.aut-an");
        }
        w.writeCharacters(id);

        w.writeEndElement();
        w.writeCharacters("\n");
    }


    public void writeMainHist(XMLStreamWriter w) throws Exception {
        w.writeStartElement("mainhist");
        w.writeCharacters("\n");

        String createDate = getFirstControlField("008").getData().substring(0, 6);
        if ("6789".contains(createDate.substring(0, 1))) {
            writeMainEvent(w, "create", "19" + createDate, createDate);
        } else {
            writeMainEvent(w, "create", "20" + createDate, createDate);
        }

        String updateDate = getFirstControlField("005").getData();
        String isoDate = updateDate.substring(0, 8) + "T" + updateDate.substring(8);
        writeMainEvent(w, "update", isoDate, updateDate);

        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writeMainEvent(XMLStreamWriter w, String maintype, String date, String display)
    throws Exception {

        w.writeStartElement("mainevent");
        w.writeAttribute("maintype", maintype);
        w.writeStartElement("maindate");
        w.writeAttribute("calendar", "gregorian");
        w.writeAttribute("normal", date);
        w.writeCharacters(display);
        w.writeEndElement();
        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writeLangDecl(XMLStreamWriter w) throws Exception {
        w.writeStartElement("languagedecl");
        w.writeEmptyElement("language");
        w.writeAttribute("scriptcode", "latin");

        String code = "eng";
        DataField tag040 = getFirstDataField("040");
        if (tag040 != null && tag040.getSubfield('b') != null) {
            code = tag040.getSubfield('b').getData();
        }
        w.writeAttribute("languagecode", code);

        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writeRuleDecl(XMLStreamWriter w) throws Exception {
        char rule = getFirstControlField("008").getData().charAt(10);

        if ("bcd".contains(Character.toString(rule))) {
            w.writeStartElement("ruledecl");

            w.writeStartElement("rule");
            if (rule != 'b') {
                w.writeAttribute("id", "aacr2");
                w.writeCharacters("Anglo-American Cataloging Rules, Second Edition.");
            } else {
                w.writeAttribute("id", "aacr1");
                w.writeCharacters("Anglo-American Cataloging Rules, First Edition.");
            }
            w.writeEndElement();

            w.writeEndElement();
            w.writeCharacters("\n");
        }
    }

    public void writeSourceDecl(XMLStreamWriter w) throws Exception {
        w.writeStartElement("sourcedecl");

        // record control number
        w.writeStartElement("source");
        ControlField tag003 = getFirstControlField("003");
        if (tag003 != null) {
            w.writeAttribute("ownercode", tag003.getData());
        } else {
            w.writeAttribute("ownercode", "AuCNLKIN");
        }

        String id = getFirstControlField("001").getData().replaceFirst("^0*", "");
        w.writeAttribute("syskey", id);
        w.writeCharacters("http://nla.gov.au/anbd.aut-an" + id);
        w.writeEndElement();

        // LC name authority number
        DataField tag010 = getFirstDataField("010");
        if (tag010 != null && tag010.getSubfield('a') != null) {
            id = tag010.getSubfield('a').getData();
            w.writeStartElement("source");
            w.writeAttribute("ownercode", "lcnaf");
            w.writeAttribute("syskey", id);
            w.writeCharacters(id);
            w.writeEndElement();
        }

        // Superseded number
        DataField tag019 = getFirstDataField("019");
        if (tag019 != null && tag019.getIndicator1() == '1' && tag019.getSubfield('a') != null) {
            id = tag019.getSubfield('a').getData();
            if (id.startsWith("0")) {
                w.writeStartElement("source");
                w.writeAttribute("ownercode", "AuCNLKIN");
                w.writeAttribute("syskey", id);
                w.writeCharacters(id);
                w.writeEndElement();
            }
        }

        // other system control numbers
        for (DataField tag035 : getDataField("035")) {
            Subfield subfield = tag035.getSubfield('a');
            if (subfield != null) {
                id = subfield.getData();
                if (id.startsWith("abv") || id.startsWith("(AuCNL)")) {
                    w.writeStartElement("source");
                    if (id.startsWith("abv")) {
                        w.writeAttribute("ownercode", "AuCNLKIN");
                    } else {
                        w.writeAttribute("ownercode", "AuCNL");
                    }
                    w.writeAttribute("syskey", id.replaceAll("\\(.*?\\)", ""));
                    w.writeCharacters(id);
                    w.writeEndElement();
                }
            }
        }

        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writeCondesc(XMLStreamWriter w) throws Exception {
        w.writeStartElement("condesc");
        w.writeCharacters("\n");

        writeIdentity(w);
        writeDesc(w);
        writeEacRels(w);

        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writeIdentity(XMLStreamWriter w) throws Exception {
        w.writeStartElement("identity");
        w.writeCharacters("\n");

        List<List<DataField>> names;
        switch(type) {
        case persname: {
            names = getDataFieldCJK("[14]00");
            writeGrp(w, "pers", names);
            break;
        }
        case famname: {
            names = getDataFieldCJK("[14]00");
            writeGrp(w, "fam", names);
            break;
        }
        case corpname: {
            names = getDataFieldCJK("[14]10");
            writeCorpGrp(w, names);
            break;
        }
        }

        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writeGrp(XMLStreamWriter w, String type, List<List<DataField>> allnames) throws Exception {
    		for (List<DataField> names : allnames) {
            if (names.size() > 1) {
                w.writeStartElement(type + "grp");
                w.writeCharacters("\n");
                for (DataField name : names) {
                    writeName(w, type + "head", name);
                }
                w.writeEndElement();
                w.writeCharacters("\n");
            } else if (!names.isEmpty()) {
                writeName(w, type + "head", names.get(0));
            }
        }
    }

    public void writeName(XMLStreamWriter w, String type, DataField name) throws Exception {
    		boolean primary = name.getTag().equals("100");
    	
        w.writeStartElement(type);
        if (primary) {
            w.writeAttribute("authorized", authorized);
            primary = true;
        }
        w.writeCharacters("\n");

        char ind1 = name.getIndicator1();
        Subfield subfield = name.getSubfield('a');
        String data = subfield.getData();

        if (ind1 == '3') {
            writePart(w, "familyname", data);
        } else {
            String parttype = "forename";
            String splitChar = data.contains(",") ? "," : " ";
            String[] namevals = data.split(splitChar);
            writePart(w, ind1 == '1' ? "surname" : parttype, namevals[0]);

            for (int i = 1; i < namevals.length; i++) {
            	for (String nameval : namevals[i].trim().split(" ")) {
                    writePart(w, parttype, nameval);
                }
            }
        }

        subfield = name.getSubfield('q');
        if (subfield != null) {
            writeNameAdd(w, "extension", subfield.getData());
        }

        for (Object obj : name.getSubfields('c')) {
            writeNameAdd(w, "title", ((Subfield) obj).getData());
        }

        subfield = name.getSubfield('d');
        if (subfield != null) {
            writeExistDate(w, subfield.getData());
            if (existDate == null && name.getTag().equals("100")) {
                existDate = subfield.getData();
            }
        }
        
        if (primary) {
        	writeSourceRef(w);
        	writeDescNote(w);
        }

        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writePart(XMLStreamWriter w, String type, String value) throws Exception {
        w.writeStartElement("part");
        if (type != null && !type.equals("")) {
            w.writeAttribute("type", type);
        }
        w.writeCharacters(value.replaceAll("[(),.]", ""));
        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writeNameAdd(XMLStreamWriter w, String type, String value) throws Exception {
        w.writeStartElement("nameadd");
        if (type != null && !type.equals("")) {
            w.writeAttribute("type", type);
        }
        w.writeCharacters(value.replaceAll("[(),.]", ""));
        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writeExistDate(XMLStreamWriter w, String date) throws Exception {
        w.writeStartElement("existdate");
        w.writeAttribute("calendar", "gregorian");

        // normalize the date
        date = date.trim();
        List<String> parts = new ArrayList<String>(Arrays.asList(date.split("[- ]")));
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i).replaceAll("[^0-9]", "").trim();
            parts.set(i, part.length() > 4 ? part.substring(0, 4) : part);
        }
        parts.remove("");
        
        String normal;
        if (parts.size() == 2) {
            w.writeAttribute("form", "closedspan");
            w.writeAttribute("scope", "begin-end");
            normal = parts.get(0) + "/" + parts.get(1);
        } else if (date.endsWith("-")) {
            w.writeAttribute("form", "openspan");
            w.writeAttribute("scope", "begin");
            normal = parts.get(0);
        } else if (date.startsWith("-")) {
            w.writeAttribute("form", "openspan");
            w.writeAttribute("scope", "end");
            normal = parts.get(0);
        } else {
            w.writeAttribute("form", "openspan");
            w.writeAttribute("scope", "unknown");
            normal = parts.get(0);
        }
        if (normal.length() >= 4) {
            w.writeAttribute("normal", normal);
        }

        w.writeCharacters(date);
        w.writeEndElement();
        w.writeCharacters("\n");
    }

    public void writeCorpGrp(XMLStreamWriter w, List<List<DataField>> allnames) throws Exception {
    	for (List<DataField> names : allnames) {
            if (names.size() > 1) {
                w.writeStartElement("corpgrp");
                w.writeCharacters("\n");
                for (DataField name : names) {
                    writeCorpHead(w, name);
                }
                w.writeEndElement();
                w.writeCharacters("\n");
            } else if (!names.isEmpty()) {
                writeCorpHead(w, names.get(0));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void writeCorpHead(XMLStreamWriter w, DataField name) throws Exception {
    		boolean primary = name.getTag().equals("110");
    		
        w.writeStartElement("corphead");
        if (primary) {
            w.writeAttribute("authorized", authorized);
        }
        w.writeCharacters("\n");

        char ind1 = name.getIndicator1();
        Iterator it = name.getSubfields().iterator();
        while (it.hasNext()) {
            Subfield subfield = (Subfield) it.next();
            char code = subfield.getCode();
            if ("ab".contains("" + code)) {
                String type = null;
                if (code == 'a' && ind1 == '1') {
                    type = "jurisdiction";
                } else if (code == 'b' && ind1 == '2') {
                    type = "subordinate";
                }
                writePart(w, type, subfield.getData());
            }
        }
        
        if (primary) {
        	writeSourceRef(w);
        }

        w.writeEndElement();
        w.writeCharacters("\n");
    }

    @SuppressWarnings("unchecked")
    public void writeSourceRef(XMLStreamWriter w) throws Exception {
        List<DataField> tag670s = getDataField("670");
        if (tag670s.size() > 1) {
            w.writeStartElement("sourcerefs");
            w.writeCharacters("\n");
        }

        for (DataField tag670 : tag670s) {
            w.writeStartElement("sourceref");
            w.writeCharacters(getSubfieldContents(tag670, "ab"));
            
            List<Subfield> tag670u = tag670.getSubfields('u');
            if (tag670u.size() > 0) {
            	w.writeStartElement("sourceinfo");
            	for (Subfield subU : tag670u) {
            		w.writeStartElement("extref");
            		w.writeAttribute("href", subU.getData());
            		w.writeCharacters(subU.getData());
            		w.writeEndElement();
            	}
            	w.writeEndElement();
            }
            
            w.writeEndElement();
            w.writeCharacters("\n");
        }

        if (tag670s.size() > 1) {
            w.writeEndElement();
            w.writeCharacters("\n");
        }
    }
    
    public void writeDescNote(XMLStreamWriter w) throws Exception {
      List<DataField> tag675s = getDataField("675");
      if (tag675s.size() > 1) {
          w.writeStartElement("descnotess");
          w.writeCharacters("\n");
      }

      for (DataField tag670 : tag675s) {
          w.writeStartElement("descnote");
          w.writeCharacters(getSubfieldContents(tag670, "a"));
          w.writeEndElement();
          w.writeCharacters("\n");
      }

      if (tag675s.size() > 1) {
          w.writeEndElement();
          w.writeCharacters("\n");
      }
  }

    public void writeDesc(XMLStreamWriter w) throws Exception {
        List<DataField> tag678s = getDataField("(665)|(678)");
        List<DataField> tag680s = getDataField("(666)|(680)");

        if (existDate != null || tag678s.size() > 0 || tag680s.size() > 0) {
            w.writeStartElement("desc");

            switch (type) {
            case persname: {
                writeTypeDesc(w, "pers", existDate, tag680s);
                break;
            }
            case famname: {
                writeTypeDesc(w, "fam", existDate, tag680s);
                break;
            }
            case corpname: {
                writeTypeDesc(w, "corp", existDate, tag680s);
                break;
            }
            }

            writeBiogHist(w, tag678s);

            w.writeEndElement();
            w.writeCharacters("\n");
        }
    }

    @SuppressWarnings("unchecked")
    public void writeBiogHist(XMLStreamWriter w, List<DataField> tags) throws Exception {
    	for (DataField tag : tags) {
            w.writeStartElement("bioghist");
            w.writeAttribute("ea", tag.getTag());

            Iterator it = tag.getSubfields().iterator();
            while (it.hasNext()) {
                Subfield sub = (Subfield) it.next();
                switch(sub.getCode()) {
                case 'a':
                case 'b':
                    w.writeStartElement("p");
                    w.writeCharacters(sub.getData());
                    w.writeEndElement();
                    break;
                case 'u':
                    w.writeStartElement("didentifier");
                    w.writeAttribute("href", sub.getData());
                    w.writeCharacters(sub.getData());
                    w.writeEndElement();
                    break;
                }
            }

            w.writeEndElement();
            w.writeCharacters("\n");
        }
    }

    public void writeTypeDesc(XMLStreamWriter w, String type, String date, List<DataField> tags) throws Exception {
    	if (date != null || tags.size() > 0) {
        w.writeStartElement(type + "desc");

        if (date != null) {
            w.writeStartElement("existdesc");
            writeExistDate(w, date);
            w.writeEndElement();
            w.writeCharacters("\n");
        }

        if (tags != null && tags.size() > 0) {
            writeDescentry(w, tags);
        }

        w.writeEndElement();
        w.writeCharacters("\n");
    	}
    }

    @SuppressWarnings("unchecked")
    public void writeDescentry(XMLStreamWriter w, List<DataField> tags) throws Exception {
    	for (DataField tag : tags) {
            w.writeStartElement("descentry");
            w.writeAttribute("ea", tag.getTag());

            StringBuffer buf = new StringBuffer();
            Iterator it = tag.getSubfields().iterator();
            while (it.hasNext()) {
                Subfield subfield = (Subfield) it.next();
                if (subfield.getCode() == 'i' || subfield.getCode() == 'a') {
                    buf.append(subfield.getData() + " ");
                }
            }

            w.writeStartElement("value");
            w.writeCharacters(buf.toString().trim());
            w.writeEndElement();

            w.writeEndElement();
            w.writeCharacters("\n");
        }
    }

    public void writeEacRels(XMLStreamWriter w) throws Exception {
        List<DataField> names = getDataField("5[01]0");
        if (names.size() > 0) {
            w.writeStartElement("eacrels");
            w.writeCharacters("\n");

            for (DataField name : names) {
                writeEacRel(w, name);
            }

            w.writeEndElement();
            w.writeCharacters("\n");
        }
    }

    public void writeEacRel(XMLStreamWriter w, DataField name) throws Exception {
        // determine the relationship type
        String reltype = null;
        Subfield subw = name.getSubfield('w');
        if (subw != null) {
            switch(subw.getData().charAt(0)) {
            case 'a':
                reltype = "earlier";
                break;
            case 'b':
                reltype = "later";
                break;
            case 'i':
                reltype = "associative";
                break;
            case 't':
                reltype = "superior";
                break;
            default:
                reltype = "associative";
            }
        } else {
            reltype = "associative";
        }

        w.writeStartElement("eacrel");
        if (reltype != null) {
            w.writeAttribute("reltype", reltype);
        }
        w.writeCharacters("\n");

        String type = null;
        String contents = null;
        if (name.getTag().equalsIgnoreCase("500")) {
            char ind1 = name.getIndicator1();
            if (ind1 != '3') {
                type = "pers";
            } else {
                type = "fam";
            }
            contents = getSubfieldContents(name, "acqd");
        } else if (name.getTag().equalsIgnoreCase("510")) {
            type = "corp";
            contents = getSubfieldContents(name, "ab");
        }

        w.writeStartElement(type + "name");
        w.writeCharacters(contents);
        w.writeEndElement();

        w.writeEndElement();
        w.writeCharacters("\n");
    }

    /**
     * Get the first data field whose tag no. matches the 
     * pattern.
     * @param pattern		a regular expression pattern
     * @return the first matching data field.
     */
    public DataField getFirstDataField(String pattern) {
        Iterator<DataField> it = getDataField(pattern).iterator();
        if (it.hasNext()) {
            return it.next();
        } else {
            return null;
        }
    }

    /**
     * Get all data fields whose tag no. matches the 
     * pattern.
     * @param pattern		a regular expression pattern
     * @return list of matching data field.
     */
    @SuppressWarnings(value = "unchecked")
    public List<List<DataField>> getDataFieldCJK(String pattern) {
        List<List<DataField>> list = new ArrayList<List<DataField>>(2);
        for (Object obj : record.getDataFields()) {
            DataField field = (DataField) obj;
            if (field.getTag().matches(pattern)) {
                List<DataField> cjklist = new ArrayList<DataField>(2);
                list.add(cjklist);
                cjklist.add(field);

                // check if there is a CJK alternative
                Subfield sub6 = field.getSubfield('6');
                if (sub6 != null) {
                    String[] related = sub6.getData().split("-/");
                    String expectedTagNo = field.getTag() + "-" + related[1];
                    for (DataField cjkfield : getDataField(related[0])) {
                        Subfield cjksub6 = cjkfield.getSubfield('6');
                        if (cjksub6 != null && cjksub6.getData().startsWith(expectedTagNo)) {
                            // found so clone the field
                            DataField newDataField = MarcFactory.newInstance().newDataField();
                            newDataField.setTag(field.getTag());
                            newDataField.setIndicator1(cjkfield.getIndicator1());
                            newDataField.setIndicator2(cjkfield.getIndicator2());
                            newDataField.getSubfields().add(cjkfield.getSubfields());
                            // add it to the list
                            cjklist.add(newDataField);
                        }
                    }
                }
            }
        }

        return list;
    }

    /**
     * Get all data fields whose tag no. matches the 
     * pattern.
     * @param pattern		a regular expression pattern
     * @return list of matching data field.
     */
    public List<DataField> getDataField(String pattern) {
        List<DataField> list = new ArrayList<DataField>(2);
        for (Object obj : record.getDataFields()) {
            DataField field = (DataField) obj;
            if (field.getTag().matches(pattern)) {
                list.add(field);
            }
        }

        return list;
    }

    /**
     * Get the first control field whose tag no. matches the 
     * pattern.
     * @param pattern		a regular expression pattern
     * @return the first matching data field.
     */
    public ControlField getFirstControlField(String pattern) {
        Iterator<ControlField> it = getControlField(pattern).iterator();
        if (it.hasNext()) {
            return it.next();
        } else {
            return null;
        }
    }

    /**
     * Get all control fields whose tag no. matches the 
     * pattern.
     * @param pattern		a regular expression pattern
     * @return list of matching data field.
     */
    public List<ControlField> getControlField(String pattern) {
        List<ControlField> list = new ArrayList<ControlField>(2);
        for (Object obj : record.getControlFields()) {
            ControlField field = (ControlField) obj;
            if (field.getTag().matches(pattern)) {
                list.add(field);
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public String getSubfieldContents(DataField field, String codes) {
        StringBuffer buf = new StringBuffer();

        Iterator it = field.getSubfields().iterator();
        while (it.hasNext()) {
            Subfield subfield = (Subfield) it.next();
            if (codes.contains(Character.toString(subfield.getCode()))) {
                buf.append(subfield.getData());
                buf.append(' ');
            }
        }

        return buf.toString().trim();
    }

}
