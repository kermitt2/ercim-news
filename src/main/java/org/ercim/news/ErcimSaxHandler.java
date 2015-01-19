package org.ercim.news;

import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Person;
import org.grobid.core.data.Date;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * SAX parser for the TEI format catalogue of the ERCIM collection.
 *
 * @author Patrice Lopez
 */
public class ErcimSaxHandler extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

	private String volume = null;

    private String forename = null;
    private String surname = null;
	private String middlename = null;
	private boolean first = true;

	private BiblioItem biblio = null;
	
	private List<BiblioItem> biblios = null;

    public ErcimSaxHandler() {
        biblios = new ArrayList<BiblioItem>();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }
	
	public List<BiblioItem> getBiblios() {
		return biblios;
	}

	public void setVolume(String vol) {
		volume = vol;
	}

    public void endElement(java.lang.String uri,
                           java.lang.String localName,
                           java.lang.String qName) throws SAXException {
        if (qName.equals("title")) {
			// some basic cleaning for the title
			String title = getText();
			title = title.replace("\n", " ");
			title = title .replace("  ", " ");
			biblio.setTitle(title);
        }
        else if (qName.equals("biblScope")) {
            String pageRange = getText();
			biblio.setPageRange(pageRange);
			int ind = pageRange.indexOf("-");
			if (ind == -1) {
				// the article has only one page
				int beginPage = -1;
				try {
					beginPage = Integer.parseInt(pageRange);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				if (beginPage != -1) {
					biblio.setBeginPage(beginPage);
					biblio.setEndPage(beginPage);
				}
			}
			else {
				int beginPage = -1;
				int endPage = -1;
				try {
					beginPage = Integer.parseInt(pageRange.substring(0,ind));
					endPage = Integer.parseInt(pageRange.substring(ind+1, pageRange.length()));
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				if (beginPage != -1) {
					biblio.setBeginPage(beginPage);
					biblio.setEndPage(endPage);
				}
			}
        }
        else if (qName.equals("date")) {
			Date date = new Date();
			int year = -1;
			try {
				year = Integer.parseInt(getText());
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			if (year != -1)
				date.setYear(year);
			biblio.setNormalizedPublicationDate(date);
        }
        else if (qName.equals("abstract")) {
			// some basic cleaning for the abstract
			String abst = getText();
			abst = abst.replace("\n", " ");
			abst = abst .replace("  ", " ");
			biblio.setAbstract(abst);
        }
        else if (qName.equals("biblFull")) {
            biblios.add(biblio);
        } 
        else if (qName.equals("forename")) {
			if (first) {
				forename = getText();
				// actually the middlename is put with the forename...
				int ind = forename.indexOf(" ");
				if (ind != -1) {
					middlename = forename.substring(ind+1, forename.length());
					middlename = middlename.replace(".","");
					forename = forename.substring(0, ind);
				}
			}
			else {
				if (middlename == null) {
					middlename = getText();
				}	
				else 
					middlename += " " + getText();
			}
        } 
        else if (qName.equals("surname")) {
            surname = getText();
        } 
        else if (qName.equals("persName")) {
            Person author = new Person();
			if (forename != null)
				author.setFirstName(forename);
			if (middlename != null)
				author.setMiddleName(middlename);
			if (surname != null)
				author.setLastName(surname);
			biblio.addFullAuthor(author);
			forename = null;
			middlename = null;
			surname = null;
        } 
		accumulator.setLength(0);
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
            throws SAXException {
        if (qName.equals("biblFull")) {
            biblio = new BiblioItem();
			biblio.setJournal("ERCIM News");
			if (volume != null)
				biblio.setVolumeBlock(volume, false);
			biblio.setPublisher("ERCIM, European Research Consortium for Informatics and Mathematics");
        }
		else if (qName.equals("biblScope")) {
            int length = atts.getLength();
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("pp")) {
                            //page = true;
                        }
                    }
                }
            }
        }
		else if (qName.equals("forename")) {
            int length = atts.getLength();
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("first")) {
                            first = true;
                        }
						else if (value.equals("middle")) {
                            first = false;
                        }
                    }
                }
            }
        }
		accumulator.setLength(0); 
    }

}