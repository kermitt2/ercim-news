package org.ercim.news;

import org.grobid.core.utilities.GrobidPropertyKeys;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.document.Document;
import org.grobid.core.document.BasicStructureBuilder;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.EngineParsers;
import org.grobid.core.engines.HeaderParser;
import org.grobid.core.engines.CitationParser;
import org.grobid.core.engines.Segmentation;
import org.grobid.core.engines.SegmentationLabel;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.mock.MockContext;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.exceptions.*;
import org.grobid.core.main.*;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;


/**
 * Using Grobid for Ercim.
 * 
 * @author Patrice Lopez
 *
 */
public class GrobidProcess {
	//private static Engine engine = null;
	EngineParsers parsers = null;
	
	public GrobidProcess() {
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream("ercim_news.properties"));
			String pGrobidHome = prop.getProperty("org.ercim.news.pGrobidHome");
			String pGrobidProperties = prop.getProperty("org.ercim.news.pGrobidProperties");

			MockContext.setInitialContext(pGrobidHome, pGrobidProperties);		
			GrobidProperties.getInstance();
			LibraryLoader.load();
			
			System.out.println(">>>>>>>> GROBID_HOME="+GrobidProperties.get_GROBID_HOME_PATH());
			parsers = new EngineParsers();
		}
		catch (Exception e) {
			// If an exception is generated, print a stack trace
			e.printStackTrace();
		} 
		finally {
			try {
				MockContext.destroyInitialContext();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public BiblioItem runHeader(Document doc) {
		// Biblio object for the result
		BiblioItem resHeader = new BiblioItem();
		try {
			HeaderParser parser = parsers.getHeaderParser();
			parser.processingHeaderSection(doc, false, resHeader);
		} 
		catch (Exception e) {
			// If an exception is generated, print a stack trace
			e.printStackTrace();
		} 
		return resHeader;
	}
	
	public List<BibDataSet> runReferences(Document doc) {
		List<BibDataSet> results = null;
		try {
			CitationParser parser = parsers.getCitationParser();
			results = parser.processingReferenceSection(doc, parsers.getReferenceSegmenterParser(), false);
		} 
		catch (Exception e) {
			// If an exception is generated, print a stack trace
			e.printStackTrace();
		} 
		return results;
	}
	
	public String runFullText(String pdfPath) {
		String tei = null;
		try {
			//tei = engine.fullTextToTEI(pdfPath, false, false);
		} 
		catch (Exception e) {
			// If an exception is generated, print a stack trace
			e.printStackTrace();
		} 
		return tei;
	}
	
	/**
     *  Custom segmentation for ERCIM article. This method will also extract the images in the
     *  PDF file.
	 */
  	public Document ercimSegmentation(String input, File tmpPath, BiblioItem header) {
        if (input == null) {
            throw new GrobidResourceException("Cannot process pdf file, because input file was null.");
        }
        File inputFile = new File(input);
        if (!inputFile.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because input file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
        }
        if (tmpPath == null) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        }
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                    tmpPath.getAbsolutePath() + "' does not exists.");
        }
        Document doc = new Document(input, tmpPath.getAbsolutePath());
        String pathXML = null;
        try {
            int startPage = -1;
            int endPage = -1;
            pathXML = doc.pdf2xml(true, false, startPage, endPage, input, tmpPath.getAbsolutePath(), false);
            //with timeout,
            //no force pdf reloading
            // input is the pdf absolute path, tmpPath is the temp. directory for the temp. lxml file,
            // path is the resource path
            // and we extract images in the PDF file
            if (pathXML == null) {
                throw new GrobidResourceException("PDF parsing fails, " +
                        "because path of where to store xml file is null.");
            }
            doc.setPathXML(pathXML);
            List<String> tokenizations = doc.addTokenizedDocument();

            if (doc.getBlocks() == null) {
                throw new GrobidException("PDF parsing resulted in empty content");
            }

			String content = Segmentation.getAllLinesFeatured(doc, false);
			if ( (content != null) && (content.trim().length() > 0) ) {
	            String labelledResult = ercimSegment(content, header);

	            //FileUtils.writeStringToFile(new File("/tmp/x.txt"), labelledResult);
				//FileUtils.writeStringToFile(new File("/tmp/x2.txt"), tokenizations.toString());

	            // set the different sections of the Document object
	            doc = BasicStructureBuilder.generalResultSegmentation(doc, labelledResult, tokenizations);

				System.out.println(doc.getDocumentPieceText(doc.getDocumentPart(SegmentationLabel.HEADER)));
				System.out.println("------------------");

				//System.out.println(doc.getDocumentPieceText(doc.getDocumentPart(SegmentationLabel.BODY)));
				//System.out.println("------------------");

	            //System.out.println(doc.getBlockReferences());
	            //System.out.println("------------------");
	            //System.out.println(doc.getDocumentPieceText(doc.getDocumentPart(SegmentationLabel.REFERENCES)));
				//System.out.println("------------------");
	            //LOGGER.debug(labelledResult);
				
	            //System.out.println(doc.getDocumentPieceText(doc.getDocumentPart(SegmentationLabel.FOOTNOTE)));
				//System.out.println("------------------");
			}
            return doc;
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            // keep it clean when leaving...
			
            doc.cleanLxmlFile(pathXML, false);
        }
    }
  	
	private String ercimSegment(String unlabeled, BiblioItem header) {
		try {
			FileUtils.writeStringToFile(new File("/tmp/x.txt"), unlabeled);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		StringBuffer labeling = new StringBuffer();
		
		String[] lines = unlabeled.split("\n");
		String previousTag = "<other>";
		String currentTag = null;
		for(int n=0; n<lines.length; n++) {
			String line = lines[n];
			String[] features = line.split(" ");
			String prefix = "";
			boolean bold = false;
			for(int i=0; i<features.length; i++) {
				if ( (i == 0) || (i == 1) ) {
					prefix += " " + features[i];
				}
				else if (i == 12) {
					if (features[i].equals("1"))
						bold = true;
					else
						bold = false;
				}
			}
			prefix = prefix.trim();
			
			if (header.getTitle().startsWith(prefix)) {
				currentTag = "<header>";
			}
			else if ( (previousTag.equals("<header>")) && (line.indexOf("BLOCKSTART") != -1) && 
				!bold && (!line.startsWith("by")) ) {
				currentTag = "<body>";
			}
			else if (prefix.startsWith("References") && (!previousTag.equals("<other>"))) {
				currentTag = "<references>";
			}
			else if ((prefix.startsWith("Email") || prefix.startsWith("E-mail")) 
				&& (!previousTag.equals("<reference>"))) {
				currentTag = "<other>";
			}
			else 
				currentTag = previousTag;
			
			if ( (!currentTag.equals(previousTag)) && (!currentTag.equals("<other>")) ) {
				line = line + " I-" + currentTag;
			}
			else {
				line = line + " " + currentTag;
			}
			labeling.append(line + "\n");
			previousTag = currentTag;
		}
		try {
			FileUtils.writeStringToFile(new File("/tmp/x2.txt"), labeling.toString());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return labeling.toString();
	}
	
	/**
	 * Save PDF assets (bitmap, vectorial) from a PDF
	 */
	public void saveAssets(String input, File savePath, File tmpPath) {
        if (input == null) {
            throw new GrobidResourceException("Cannot process pdf file, because input file was null.");
        }
        File inputFile = new File(input);
        if (!inputFile.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because input file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
        }
        if (tmpPath == null) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        }
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                    tmpPath.getAbsolutePath() + "' does not exists.");
        }
        if (savePath == null) {
            throw new GrobidResourceException("Cannot save assets, because asset path is null.");
        }
        if (!savePath.exists()) {
            throw new GrobidResourceException("Cannot save assets, because asset path '" +
                    savePath.getAbsolutePath() + "' does not exists.");
        }
        Document doc = new Document(input, tmpPath.getAbsolutePath());
        String pathXML = null;
        try {
            int startPage = -1;
            int endPage = -1;
            pathXML = doc.pdf2xml(true, false, startPage, endPage, input, tmpPath.getAbsolutePath(), true);
            //with timeout,
            //no force pdf reloading
            // input is the pdf absolute path, tmpPath is the temp. directory for the temp. lxml file,
            // path is the resource path
            // and we extract images in the PDF file
            if (pathXML == null) {
                throw new GrobidResourceException("PDF parsing fails, " +
                        "because path of where to store xml file is null.");
            }
			// save the assets to the specified path
			File asset_path = new File(pathXML + "_data");
			
            File[] refFiles = asset_path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
					// change here for controling image filtering
					if (name.endsWith(".vec"))
                    	return false; 
					else 
						return true;
                }
            });

            if (refFiles == null)
                return;
			
            for (File assetFile : refFiles) {
                String name = assetFile.getName();
				
				if (name.endsWith(".pbm")) {
					//conversion into png
					try {
						System.out.println(assetFile.getPath());
						final BufferedImage pbm = ImageIO.read(assetFile);
					   	File output = new File(savePath.getAbsolutePath() + "/" + name.replace(".pbm",".png"));
			            if (pbm != null) {
			                ImageIO.write(pbm, "png", output);
			            }
						else {
							System.out.println("Fail reading file: " + assetFile.getPath());
						}
			        } 
					catch (Exception e) {
			            System.out.println("Error converting: " + assetFile.getPath());
			        }
				}
				else {
					// simple file copy
					File output = new File(savePath.getAbsolutePath() + "/" + name);
					FileUtils.copyFile(assetFile, output);
				}
			}	
		}
		catch (Exception e) {
		  	throw new GrobidException("An exception occurred while running Grobid.", e);
		} 
		finally {
            // keep it clean when leaving...
            doc.cleanLxmlFile(pathXML, true);
        }
	}
}