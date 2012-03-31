package sdp.common;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests some functions from the sdp.common.Utilities class
 * @author mihaela_laura_ionescu
 *
 */
public class UtilitiesTest {

	@Test
	/**
	 * @see sdp.common.Utilities#stripString(String string)
	 */
	public void testStripString() {
		assertEquals(Utilities.stripString(""), "");
		assertEquals(Utilities.stripString("     this is a test string"), "this is a test string");
	}

	@Test
	/**
	 * @see sdp.common.Utilities#valueWithinBounds(int value, int lower, int upper)
	 */
	public void testValueWithinBounds(){
		assertTrue(Utilities.valueWithinBounds(12, 10, 34));
		assertTrue(Utilities.valueWithinBounds(-12, 34, 10));
		assertTrue(Utilities.valueWithinBounds(-1, 1, 0));
	}
	
	
}
