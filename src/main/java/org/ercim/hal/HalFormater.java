package org.ercim.hal;

import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Person;
import org.grobid.core.data.Date;
import org.grobid.core.data.Affiliation;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

import org.apache.commons.io.FileUtils;
import org.grobid.core.document.*;

/**
 * Output of a biblio item corresponding to a header in the HAL import format. 
 * 
 * @author Patrice Lopez
 *
 */
public class HalFormater {
	
	public org.grobid.core.document.Document doc = null;
	
	public HalFormater(org.grobid.core.document.Document doc) {
		this.doc = doc;
	}
	
	public StringBuilder format(BiblioItem biblioCatalogue, BiblioItem biblioExtracted, List<String> assets) {
		StringBuilder builder = new StringBuilder();
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		builder.append("<TEI xmlns=\"http://www.tei-c.org/ns/1.0\" xmlns:hal=\"http://hal.archives-ouvertes.fr/\">");
		builder.append("\t<text>\n\t\t<body>\n\t\t\t<listBibl>\n\t\t\t\t<biblFull>\n");
		builder.append("\t\t\t\t\t<titleStmt>\n");
		// add lang attribute xml:lang="en" before the title
		builder.append("\t\t\t\t\t\t<title xml:lang=\""+biblioExtracted.getLanguage()+"\">"+
			biblioCatalogue.getTitle()+"</title>\n");
		for(Person author : biblioCatalogue.getFullAuthors()) {
			if (author.getCorresp())
				builder.append("\t\t\t\t\t\t<author role=\"crp\">\n");
			else
				builder.append("\t\t\t\t\t\t<author role=\"aut\">\n");
			builder.append("\t\t\t\t\t\t\t<persName>\n");
		    builder.append("\t\t\t\t\t\t\t\t<forename type=\"first\">"+author.getFirstName()+"<forename>\n");
		    builder.append("\t\t\t\t\t\t\t\t<surname>"+author.getLastName()+"</surname>\n");
		    builder.append("\t\t\t\t\t\t\t</persName>\n");
			if ( (author.getEmail() != null) && (author.getEmail().trim().length()>0) )
				builder.append("\t\t\t\t\t\t\t<email>"+author.getEmail()+"</email>\n");
		    //<!-- Info pour Patrice Lopez, pour les affiliations mettre l'info comme suit : nom établissement, pays dans la balise affiliation-->
		    //<!-- Info pour Patrice Lopez, si tu as nom, prénom et mail alors rajouter la balise auteur correspondant -->
		     //<affiliation ref="#struct-300009"></affiliation>
			 if (author.getAffiliations() != null) {
				List<Affiliation> affiliations = author.getAffiliations();
				for(Affiliation affiliation : affiliations) {
					builder.append("\t\t\t\t\t\t\t" + affiliation.toTEI() +"\n");
					//builder.append("\t\t\t\t\t\t\t<affiliation>"++"</affiliation>\n");
			 	}
			 }
		     builder.append("\t\t\t\t\t\t</author>\n");
		}
		builder.append("\t\t\t\t\t</titleStmt>\n");
		builder.append("\t\t\t\t\t<editionStmt>\n");
		//<!-- Pour Patrice Lopez,Insérer autant de lignes "edition ref" que de fichiers à importer ; indiquer le nom du fichier dans l'attribut "target" ; préfixer les fichiers avec le n° d'article tel que : art-41 pour qu'aucun du projet fichier n'ait le même nom -->
		builder.append("\t\t\t\t\t\t<edition>\n");
		builder.append("\t\t\t\t\t\t\t<ref type=\"file\" subtype=\"greenPublisher\" target=\"ftp://ftp.ccsd.cnrs.fr/"
			+biblioCatalogue.getVolume()+"-"+biblioCatalogue.getBeginPage()+".pdf\" n=\"1\"/>\n");  
		builder.append("\t\t\t\t\t\t\t<ref type=\"annex\" subtype=\"greenPublisher\" target=\"ftp://ftp.ccsd.cnrs.fr/"+biblioCatalogue.getVolume()+"-"+biblioCatalogue.getBeginPage()+".citations-grobid.bib\" n=\"0\"/>\n");
		if (assets != null) {
			for(String asset : assets) {
				builder.append("\t\t\t\t\t\t\t<ref type=\"annex\" subtype=\"greenPublisher\" target=\"ftp://ftp.ccsd.cnrs.fr/"+asset+"\" n=\"1\"/>\n");
			}
		}
		builder.append("\t\t\t\t\t</editionStmt>\n");
		builder.append("\t\t\t\t\t<publicationStmt>\n");
		builder.append("\t\t\t\t\t\t<availability>\n");
		builder.append("\t\t\t\t\t\t\t<licence target=\"http://creativecommons.org/licenses/by/\"/>\n");
		builder.append("\t\t\t\t\t\t</availability>\n");
		builder.append("\t\t\t\t\t</publicationStmt>\n");
		builder.append("\t\t\t\t\t<seriesStmt>\n");
		builder.append("\t\t\t\t\t\t<idno type=\"stamp\" n=\"ERCIM-NEWS-"+biblioCatalogue.getVolume()+ "\"/>\n");
		builder.append("\t\t\t\t\t\t<idno type=\"stamp\" n=\"ERCIM-NEWS\"/>\n");
		builder.append("\t\t\t\t\t\t<idno type=\"stamp\" n=\"ERCIM\"/>\n");
		builder.append("\t\t\t\t\t</seriesStmt>\n");
		builder.append("\t\t\t\t\t<notesStmt>\n");
		builder.append("\t\t\t\t\t\t<note type=\"audience\" n=\"2\"/>\n");
		builder.append("\t\t\t\t\t\t<note type=\"popular\" n=\"1\"/>\n");
		builder.append("\t\t\t\t\t\t<note type=\"peer\" n=\"1\"/>\n");
		builder.append("\t\t\t\t\t</notesStmt>\n");
		builder.append("\t\t\t\t\t<sourceDesc>\n");
		builder.append("\t\t\t\t\t\t<biblStruct>\n");
		builder.append("\t\t\t\t\t\t\t<analytic>\n");
		builder.append("\t\t\t\t\t\t\t\t<title xml:lang=\""+biblioExtracted.getLanguage()+"\">"+
			biblioCatalogue.getTitle()+"</title>\n");
		for(Person author : biblioCatalogue.getFullAuthors()) {
			if (author.getCorresp())
				builder.append("\t\t\t\t\t\t\t\t<author role=\"crp\">\n");
			else
				builder.append("\t\t\t\t\t\t\t\t<author role=\"aut\">\n");
		    builder.append("\t\t\t\t\t\t\t\t\t<persName>\n"); 
		    builder.append("\t\t\t\t\t\t\t\t\t\t<forename type=\"first\">"+author.getFirstName()+"</forename>\n");
		    builder.append("\t\t\t\t\t\t\t\t\t\t<surname>"+author.getLastName()+"</surname>\n");
		    builder.append("\t\t\t\t\t\t\t\t\t</persName>\n");
			if ( (author.getEmail() != null) && (author.getEmail().trim().length()>0) )
				builder.append("\t\t\t\t\t\t\t\t\t<email>"+author.getEmail()+"</email>\n");
		    //builder.append("\t\t\t\t\t\t\t\t\t<affiliation ref=\"#struct-300009\"></affiliation>\n");
			if (author.getAffiliations() != null) {
				List<Affiliation> affiliations = author.getAffiliations();
				for(Affiliation affiliation : affiliations) {
					builder.append("\t\t\t\t\t\t\t\t\t" + affiliation.toTEI() +"\n");
					//builder.append("\t\t\t\t\t\t\t<affiliation>"++"</affiliation>\n");
			 	}
			}
		    builder.append("\t\t\t\t\t\t\t\t</author>\n");
		    //<!-- si tu as nom, prénom et mail alors le role="crp" ; utilisable une seule fois -->
		    //<author role="crp">
		}
		builder.append("\t\t\t\t\t\t\t</analytic>\n");
		builder.append("\t\t\t\t\t\t\t<monogr>\n");
		builder.append("\t\t\t\t\t\t\t\t<idno type=\"halJournalId\">20883</idno>\n");
		builder.append("\t\t\t\t\t\t\t\t<idno type=\"issn\">0926-4981</idno>\n");   
		builder.append("\t\t\t\t\t\t\t\t<imprint>\n");
		builder.append("\t\t\t\t\t\t\t\t\t<publisher>ERCIM</publisher>\n");
		builder.append("\t\t\t\t\t\t\t\t\t\t<biblScope unit=\"issue\">"+biblioCatalogue.getVolume()+"</biblScope>\n");
		builder.append("\t\t\t\t\t\t\t\t\t\t<biblScope unit=\"pp\">"+
			biblioCatalogue.getBeginPage()+"-"+biblioCatalogue.getEndPage()+"</biblScope>\n");
		
        Date date = biblioCatalogue.getNormalizedPublicationDate();
        int year = date.getYear();
		
		builder.append("\t\t\t\t\t\t\t\t\t\t<date type=\"datePub\">"+year+"</date>\n");
		builder.append("\t\t\t\t\t\t\t\t</imprint>\n");
		builder.append("\t\t\t\t\t\t\t</monogr>\n");
		builder.append("\t\t\t\t\t\t</biblStruct>\n");
		builder.append("\t\t\t\t</sourceDesc>\n");
		builder.append("\t\t\t\t\t<profileDesc>\n");
		builder.append("\t\t\t\t\t\t<langUsage>\n");
		builder.append("\t\t\t\t\t\t\t<language ident=\""+biblioExtracted.getLanguage()+"\"></language>\n");
		builder.append("\t\t\t\t\t\t</langUsage>\n");
		builder.append("\t\t\t\t\t\t<textClass>\n");
		builder.append("\t\t\t\t\t\t\t<classCode scheme=\"halDomain\" n=\"info\"></classCode>\n");
		builder.append("\t\t\t\t\t\t\t<classCode scheme=\"halTypology\" n=\"ART\"></classCode>\n");
		builder.append("\t\t\t\t\t\t</textClass>\n");
		builder.append("\t\t\t\t\t\t<abstract xml:lang=\""+biblioExtracted.getLanguage()+
			"\">"+biblioCatalogue.getAbstract()+"</abstract>\n");
		// we don't have keywords for ERCIM
		builder.append("\t\t\t\t\t</profileDesc>\n");
		builder.append("\t\t\t\t</biblFull>\n");
		builder.append("\t\t\t</listBibl>\n");
		builder.append("\t\t</body>\n");
		builder.append("\t</text>\n");
		builder.append("</TEI>");
		
		return builder;
	}
}