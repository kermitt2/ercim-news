package org.ercim.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.utilities.GrobidPropertyKeys;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.document.*;
import org.grobid.core.engines.Engine;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.mock.MockContext;
import org.grobid.core.utilities.GrobidProperties;

import java.util.Properties;
import java.util.List;
import java.io.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Process an ERCIM collection based on the PDF and a catalogue of each issue. 
 * 
 * @author Patrice Lopez
 *
 */
public class ErcimProcess {
	private static final Logger LOGGER = LoggerFactory.getLogger(ErcimProcess.class);
	
	private GrobidProcess grobidProcess = null;
	private File tmpPath = null;
	
	public ErcimProcess() {
		grobidProcess = new GrobidProcess();
		tmpPath = GrobidProperties.getTempPath();
	}
	
	public void run() {
		try {
			String resourcesPath = "resources/input";
            File path = new File(resourcesPath + "/tei");
            // we process all tei files in the input directory
			
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".tei") || name.endsWith(".tei.xml") || name.endsWith(".xml");
                }
            });

            if (refFiles == null)
                return;
			
            for (File teifile : refFiles) {
                String name = teifile.getName();
				System.out.println("Parsing " + name);
			
				// get the volume number
				String volume = name.replace(".xml", "").replace("ercim","");
			
	            ErcimSaxHandler saxHandler = new ErcimSaxHandler();
				saxHandler.setVolume(volume);
	            // get a factory
	            SAXParserFactory spf = SAXParserFactory.newInstance();
	            //get a new instance of parser
	            SAXParser parser = spf.newSAXParser();
	            parser.parse(teifile, saxHandler);

	            List<BiblioItem> biblios = saxHandler.getBiblios();
				
				// PDF file of the volume
				String pdfPath = "resources/input/pdf/EN" + volume + "-web.pdf";
	            File pdf = new File(pdfPath);
				
				if (!pdf.exists()) {
					LOGGER.error("pdf file not found: " + pdfPath);
					System.out.println("ERROR: pdf file not found: " + pdfPath);
					continue;
				}
				
				InputStream pdfInput = new FileInputStream(pdf);
				
				// realise the PDF segmentation
				for(BiblioItem biblio : biblios) {
					//System.out.println(biblio.toTEI(1));
					
					//sub-directory for the volume if not exist
					String outPath = "resources/output/" + volume;
					File outDirectory = new File(outPath);
					if (!outDirectory.exists()) {
						outDirectory.mkdir();
					}
					//sub-directory for the article if not exist
					String outArticlePath = outPath + "/" + biblio.getBeginPage();
					File outArticleDirectory = new File(outArticlePath);
					if (!outArticleDirectory.exists()) {
						outArticleDirectory.mkdir();
					}
					
					String outPDFPath = outArticlePath + "/" + biblio.getBeginPage() + ".pdf";
					OutputStream pdfOutput = new FileOutputStream(outPDFPath);
					splitPDF(pdfPath, pdfOutput, biblio.getBeginPage(), biblio.getEndPage());
					
					// output the header in TEI
					org.grobid.core.document.Document doc = 
						new org.grobid.core.document.Document(outPDFPath, tmpPath.getAbsolutePath());
					TEIFormater teiFormater = new TEIFormater(doc);
					StringBuffer tei = teiFormater.toTEIHeader(biblio, true, false, null);
		            tei.append("\t</text>\n");
		            tei.append("</TEI>\n");
					File teiHeaderFile = new File(outArticlePath + "/" + "header.tei.xml");
					FileUtils.writeStringToFile(teiHeaderFile, tei.toString(), "UTF-8");
					
					// output the header in BibTeX
					File bibTexHeaderFile = new File(outArticlePath + "/" + "header.bib");
					FileUtils.writeStringToFile(bibTexHeaderFile, biblio.toBibTeX(), "UTF-8");
					
					// save the pdf file assets
					String outputAssetsPath = outArticlePath + "/assets";
					File outAssetsDirectory = new File(outputAssetsPath);
					if (!outAssetsDirectory.exists()) {
						outAssetsDirectory.mkdir();
					}
					grobidProcess.saveAssets(outPDFPath, outAssetsDirectory, tmpPath);
					
					// custom segmentation for ERCIM News
					doc = grobidProcess.ercimSegmentation(outPDFPath, tmpPath, biblio);
					
					// extract and enrich the header with Grobid
					BiblioItem extractedHeader = grobidProcess.runHeader(doc);
					
					// output the extended header in TEI
					tei = teiFormater.toTEIHeader(extractedHeader, true, false, null);
					tei.append("\t</text>\n");
					tei.append("</TEI>\n");
					File grobidHeaderFile = new File(outArticlePath + "/" + "header-grobid.tei.xml");
					//FileUtils.writeStringToFile(grobidHeaderFile, tei.toString(), "UTF-8");					
					
					// extract the bib references with Grobid 
					List<BibDataSet> citations = grobidProcess.runReferences(doc);
					
					// output the references in TEI
					StringBuffer result = new StringBuffer();
					// dummy header
					result.append("<?xml version=\"1.0\" ?>\n<TEI xmlns=\"http://www.tei-c.org/ns/1.0\" " + 	
					"xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
            		"\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");
					result.append("\t<teiHeader>\n\t\t<fileDesc xml:id=\"f_" + volume + "_" + biblio.getBeginPage() + 
						"\"/>\n\t</teiHeader>\n");
					result.append("\t<text>\n\t\t<front/>\n\t\t<body/>\n\t\t<back>\n\t\t\t<listBibl>\n");
					for(BibDataSet res : citations) {
						if ( (res.getResBib() != null) && (res.getResBib().getTitle() != null) ) {
							result.append(res.toTEI());
							result.append("\n");
						}
					}
					result.append("\t\t\t</listBibl>\n\t\t</back>\n\t</text>\n</TEI>\n");
					File grobidCitationsFile = new File(outArticlePath + "/" + "citations-grobid.tei.xml");
					FileUtils.writeStringToFile(grobidCitationsFile, result.toString(), "UTF-8");	
					
					// output the references in BibTeX
					StringBuffer bibtex = new StringBuffer();
					int ind = 0;
					for(BibDataSet bib : citations) {
						if ( (bib.getResBib() != null) && (bib.getResBib().getTitle() != null) ) {
							bibtex.append(bib.getResBib().toBibTeX("ref_"+ind));
							ind++;
						}
					} 
					File bibTexCitationsFile = new File(outArticlePath + "/" + "citations-grobid.bib");
					FileUtils.writeStringToFile(bibTexCitationsFile, bibtex.toString(), "UTF-8");
					
					// extract the fulltext with Grobid
					
					// output the fulltext in TEI
					
					
					
					
				}
				
			}
			
		
			
		} 
		catch (Exception e) {
			// If an exception is generated, print a stack trace
			e.printStackTrace();
		} 
		finally {
		}
	}
	
	/**
	 * Split a PDF using iText
	 * 
	 * @param inputStream Input PDF file
	 * @param outputStream Output PDF file
	 * @param fromPage start page from input PDF file
	 * @param toPage end page from input PDF file
	 */
	public static void splitPDF(String inputFileName,
	        OutputStream outputStream, int fromPage, int toPage) {
	    com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
	    try {
	        PdfReader inputPDF = new PdfReader(inputFileName);
 
	        int totalPages = inputPDF.getNumberOfPages();
 
	        //make fromPage equals to toPage if it is greater
	        if(fromPage > toPage ) {
	            fromPage = toPage;
	        }
	        if(toPage > totalPages) {
	            toPage = totalPages;
	        }
 
	        // Create a writer for the outputstream
	        PdfWriter writer = PdfWriter.getInstance(doc, outputStream);
 
	        doc.open();
	        PdfContentByte cb = writer.getDirectContent(); // Holds the PDF data
	        PdfImportedPage page;
 
	        while(fromPage <= toPage) {
	            doc.newPage();
	            page = writer.getImportedPage(inputPDF, fromPage);
	            cb.addTemplate(page, 0, 0);
	            fromPage++;
	        }
	        outputStream.flush();
	        doc.close();
	        outputStream.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        if (doc.isOpen())
	            doc.close();
	        try {
	            if (outputStream != null)
	                outputStream.close();
	        } catch (IOException ioe) {
	            ioe.printStackTrace();
	        }
	    }
	}
	
	/**
     *	
     */
    public static void main(String[] args) {
		try {	
			ErcimProcess process = new ErcimProcess();
			process.run();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}