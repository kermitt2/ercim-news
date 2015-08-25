package org.ercim.news;

import org.grobid.core.utilities.GrobidPropertyKeys;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.Person;
import org.grobid.core.data.Affiliation;
import org.grobid.core.document.*;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.EngineParsers;
import org.grobid.core.engines.HeaderParser;
import org.grobid.core.engines.CitationParser;
import org.grobid.core.engines.AffiliationAddressParser;
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
			Document resultDoc = parsers.getFullTextParser().processing(pdfPath, false, 
							false, 0, null, -1, -1, false, true);
			tei = resultDoc.getTei();
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
		
        DocumentSource documentSource = DocumentSource.fromPdf(new File(input));
		Document doc = new Document(documentSource);
		
        //Document doc = new Document(input, tmpPath.getAbsolutePath());
        String pathXML = null;
        try {
            List<String> tokenizations = doc.addTokenizedDocument();

            if (doc.getBlocks() == null) {
                throw new GrobidException("PDF parsing resulted in empty content");
            }

			String content = Segmentation.getAllLinesFeatured(doc);
			if ( (content != null) && (content.trim().length() > 0) ) {
	            String labelledResult = ercimSegment(content, header);

	            // set the different sections of the Document object
	            doc = BasicStructureBuilder.generalResultSegmentation(doc, labelledResult, tokenizations);

				System.out.println(doc.getDocumentPieceText(doc.getDocumentPart(SegmentationLabel.HEADER)));
				System.out.println("------------------");

				try {
					FileUtils.writeStringToFile(new File("/tmp/x_" + header.getBeginPage() + ".txt"), tokenizations.toString());
				}
				catch(Exception e) {
					e.printStackTrace();
				}

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
            documentSource.close(true);
        }
    }
  	
	private String ercimSegment(String unlabeled, BiblioItem header) {
		/*try {
			FileUtils.writeStringToFile(new File("/tmp/x.txt"), unlabeled);
		}
		catch(Exception e) {
			e.printStackTrace();
		}*/
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
			
			if (header.getTitle().toLowerCase().startsWith(prefix.toLowerCase()) 
				&& (previousTag.equals("<other>"))) {
				currentTag = "<header>";
			}
			else if ( (previousTag.equals("<header>")) && (line.indexOf("BLOCKSTART") != -1) 
				&& (!bold) && (!line.startsWith("by")) ) {
				currentTag = "<body>";
			}
			else if (prefix.startsWith("Reference") && (!previousTag.equals("<other>"))) {
				currentTag = "<references>";
			}
			else if ((prefix.startsWith("Email") || prefix.startsWith("E-mail") 
				|| prefix.startsWith("Please contact:")) 
				&& (previousTag.equals("<references>"))) {
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
			FileUtils.writeStringToFile(new File("/tmp/x2_" + header.getBeginPage() + ".txt"), labeling.toString());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return labeling.toString();
	}
	
	/**
	 * Save PDF assets (bitmap, vectorial) from a PDF and return the list of asset file names
	 */
	public List<String> saveAssets(String input, String savePathString) {
        if (input == null) {
            throw new GrobidResourceException("Cannot process pdf file, because input file was null.");
        }
        File inputFile = new File(input);
        if (!inputFile.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because input file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
        }
        /*if (tmpPath == null) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        }
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                    tmpPath.getAbsolutePath() + "' does not exists.");
        }*/
        if (savePathString == null) {
            throw new GrobidResourceException("Cannot save assets, because asset path is null.");
        }
        /*if (!savePath.exists()) {
            throw new GrobidResourceException("Cannot save assets, because asset path '" +
                    savePath.getAbsolutePath() + "' does not exists.");
        }*/
		
		List<String> results = new ArrayList<String>();
        DocumentSource documentSource = DocumentSource.fromPdfWithImages(new File(input), -1, -1);
		Document doc = new Document(documentSource);
        String pathXML = null;
        try {
			pathXML = documentSource.getXmlFile().getPath();
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
                return null;
			
            for (File assetFile : refFiles) {
                String name = assetFile.getName();
				
				if (name.endsWith(".pbm")) {
					//conversion into png
					try {
						System.out.println(assetFile.getPath());
						final BufferedImage pbm = ImageIO.read(assetFile);
					   	File output = new File(savePathString + "." + name.replace(".pbm",".png"));
			            if (pbm != null) {
			                ImageIO.write(pbm, "png", output);
							results.add(output.getName());
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
					File output = new File(savePathString + "." + name);
					FileUtils.copyFile(assetFile, output);
					results.add(output.getName());
				}
			}	
		}
		catch (Exception e) {
		  	throw new GrobidException("An exception occurred while running Grobid.", e);
		} 
		finally {
            // keep it clean when leaving...
            documentSource.close(true);
        }
		return results;
	}
	
	public BiblioItem getContactChunk(Document doc, BiblioItem biblio) {
		//List<String> segmentation = doc.getTokenizations();
		List<String> segmentation = doc.addTokenizedDocument();
		int pos = -1;
		for(int i=segmentation.size()-1; i>=0; i--) {
			if ((segmentation.size() > i+2) && 
				segmentation.get(i).equals("Please") && 
				segmentation.get(i+2).equals("contact")) {
				pos = i;
				break;
			}
		}
		StringBuilder builder = new StringBuilder(); 
		if (pos != -1) {
			for(int i=pos; pos<segmentation.size(); i++) {
				builder.append(segmentation.get(i));
				if ( (segmentation.get(i) == "\n") && (segmentation.get(i-1) == "\n") ) {
					break;
				}
			}
			String chunk = builder.toString();
			String chunkLow = chunk.toLowerCase();
			System.out.println(chunk);
			// first the corresp author
			boolean crp = false;
			List<Person> authors = biblio.getFullAuthors();
			for(Person author : authors) {
				String lastName = author.getLastName().toLowerCase();
				int ind1 = chunkLow.indexOf(lastName);
				if ( (ind1 == -1) && lastName.contains("ö") ) {
					ind1 = chunkLow.indexOf(lastName.replace("ö", "oe"));
				}
				if (ind1 != -1) {
					if (!crp) {
						author.setCorresp(true);
						crp = true;
					}
					int ind2 = chunkLow.indexOf("mail", ind1+1);
					if (ind2 != -1) {
						int ind3 = chunkLow.indexOf(" ", ind2+1);
						if (ind3 != -1) {
							int ind4 = chunkLow.indexOf("\n", ind3+1);
							if (ind4 != -1) {
								String emailString = chunk.substring(ind3+1, ind4).trim(); 
								if (authors.size() == 1) {
									author.setEmail(emailString);
								}
								else {
									// evaluate arity of email field
									if (emailString.contains(";") || 
										emailString.contains(",") || emailString.contains(" and ")) {
											emailString = emailString.replace(",", ";");
											biblio.setEmail(emailString);
											biblio.attachEmails();
									}
									else {
										author.setEmail(emailString);
									}
								}
							}
						}
					}
					int ind22 = chunkLow.indexOf("tel", ind1+1);
					if (ind22 != -1) {
						ind2 = ind22;
					}
					if (ind2 != -1) {
						String affiliationChunk = chunk.substring(ind1+lastName.length(), ind2);
						if (affiliationChunk.length() > 5) {
							List<Affiliation> affiliations = 
								parsers.getAffiliationAddressParser().processing(affiliationChunk);
							fixAffiliation(affiliations, authors);
							author.setAffiliations(affiliations);
						}
					}
				}
			}
		}
		return biblio;
	}
	
	private void fixAffiliation(List<Affiliation> affiliations, List<Person> authors) {
		// we have two typical errors that we can fix as post-processing: author name in affiliation
		// and Inria recognized as settlement (because it was INRIA before)...  
		for(Affiliation affiliation : affiliations) {
			if (authors.size() > 1) {
				for(Person author : authors) {
					List<String> newInstitutions = new ArrayList<String>();
					if (affiliation.getInstitutions() != null) {
						for(String institution : affiliation.getInstitutions()) {
							if (institution.contains(author.getLastName()) && 
								institution.contains(author.getFirstName())) {
								institution = institution.replace(author.getLastName(), " ");
								institution = institution.replace(author.getFirstName(), " ");
								institution = institution.trim();	
							}
							if (institution.length() > 0)
								newInstitutions.add(institution);
						}
						affiliation.setInstitutions(newInstitutions);
					}
					
					List<String> newDepartments = new ArrayList<String>();
					if (affiliation.getDepartments() != null) {
						for(String department : affiliation.getDepartments()) {
							if (department.contains(author.getLastName()) && 
								department.contains(author.getFirstName())) {
								department = department.replace(author.getLastName(), " ");
								department = department.replace(author.getFirstName(), " ");
								department = department.trim();	
							}
							if (department.length() > 0)
								newDepartments.add(department);
						}
						affiliation.setDepartments(newDepartments);
					}
					
					List<String> newLaboratories = new ArrayList<String>();
					if (affiliation.getLaboratories() != null) {
						for(String laboratory : affiliation.getLaboratories()) {
							if (laboratory.contains(author.getLastName()) && 
								laboratory.contains(author.getFirstName())) {
								laboratory = laboratory.replace(author.getLastName(), " ");
								laboratory = laboratory.replace(author.getFirstName(), " ");
								laboratory = laboratory.trim();	
							}
							if (laboratory.length() > 0)
								newLaboratories.add(laboratory);
						}
						affiliation.setLaboratories(newLaboratories);
					}
				}
			}
			if (affiliation.getSettlement() != null) {
				if (affiliation.getSettlement().toLowerCase().equals("inria")) {
					affiliation.addInstitution("Inria");
					affiliation.setSettlement(null);
				}
			}
		}
	}
	
}