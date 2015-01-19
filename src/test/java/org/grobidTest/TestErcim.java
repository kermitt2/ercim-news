package org.ercim.new;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 *  @author Patrice Lopez
 */
public class TestErcim {

	private String testPath = null;

	@Test
	public void TestErcim() throws Exception {
		String pdfPath = "./src/test/resources/EN100-web.pdf";
		assertNotNull(pdfPath);
		
	}
	
}