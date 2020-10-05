package eu.starcross.ukmaps;

import org.junit.Test;
import static org.junit.Assert.assertEquals;


/**
 * Test BitMapProvider class
 */
public class BitMapProviderTest {
    @Test
    public void getFileNames() throws Exception {

        BitmapProviderPicasso bpp = new BitmapProviderPicasso("https://test.dev/");

        String filename = bpp.convertFilename(1045, 2180);
        assertEquals("TL/TL20NW59.gif", filename);

    }
}