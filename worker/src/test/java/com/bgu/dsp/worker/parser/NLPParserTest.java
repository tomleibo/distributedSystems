package com.bgu.dsp.worker.parser;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by hagai_lvi on 12/04/2016.
 */
public class NLPParserTest {

	public static final String TWEET = "This Weekend: Beijing, China - Dec 4 at Tango Club";

	@Test
	public void testGetSentiment() throws Exception {
		int sentiment = NLPParser.getSentiment(TWEET);
		Assert.assertEquals(2, sentiment);
	}

	@Test
	public void testGetEntities() throws Exception {
		Set<String> expected = new HashSet<String>();
		expected.add("Hamburger:ORGANIZATION");
		expected.add("Marys:ORGANIZATION");
		expected.add("Tampa:ORGANIZATION");

		Set<String> entities = NLPParser.getEntities(TWEET);
		Assert.assertEquals(expected, entities);
	}
}