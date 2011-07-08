package org.kc7bfi.jflac.metadata;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.kc7bfi.jflac.FLACDecoder;

public class MetadataTest extends TestCase {

    private static final File TEST_ROOT = new File("src" + File.separator
            + "test" + File.separator + "resources" + File.separator
            + "testdata");
    private static final File TEST_FILE = new File(TEST_ROOT, "mustang.flac");

    private FileInputStream in;

    public void setUp() {
        try {
            in = new FileInputStream(TEST_FILE);
        } catch (IOException ioe) {
            Assert.fail("Unable to read the test file: " + ioe.toString());
        }
    }

    public void tearDown() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) {
            }
        }
    }

    public void testPictureMetadata() throws IOException {
        FLACDecoder d = new FLACDecoder(in);
        Metadata[] metadata = d.readMetadata();
        
        byte[] picture1 = readImage(new File(TEST_ROOT, "flac.png"));
        byte[] picture2 = readImage(new File(TEST_ROOT, "flac-icon.png"));

        boolean picture1Found = false;
        boolean picture2found = false;
        
        for (Metadata md : metadata) {
            if (md instanceof Picture) {
                Picture p = (Picture) md;
                byte[] picture = p.getImage();
                
                if ("flac.png".equals(p.getDescString())) {
                    picture1Found = true;
                    Assert.assertEquals(3, p.getPictureType());
                    Assert.assertEquals("image/png", p.getMimeString());
                    Assert.assertEquals("flac.png", p.getDescString());
                    Assert.assertEquals(262, p.getPicPixelWidth());
                    Assert.assertEquals(130, p.getPicPixelHeight());
                    Assert.assertEquals(0, p.getPicBitsPerPixel());
                    Assert.assertEquals(0, p.getPicColorCount());
                    Assert.assertTrue(Arrays.equals(picture, picture1));
                } else if ("flac-icon.png".equals(p.getDescString())) {
                    picture2found = true;
                    Assert.assertEquals(19, p.getPictureType());
                    Assert.assertEquals("image/png", p.getMimeString());
                    Assert.assertEquals("flac-icon.png", p.getDescString());
                    Assert.assertEquals(32, p.getPicPixelWidth());
                    Assert.assertEquals(32, p.getPicPixelHeight());
                    Assert.assertEquals(0, p.getPicBitsPerPixel());
                    Assert.assertEquals(0, p.getPicColorCount());
                    Assert.assertTrue(Arrays.equals(picture, picture2));
                } else {
                    Assert.fail("Unkown picture found");
                }
            }
        }

        Assert.assertTrue("Both picture should have been found", picture1Found && picture2found);
    }

    public void testVorbisComments() throws IOException {
        FLACDecoder d = new FLACDecoder(in);
        Metadata[] metadata = d.readMetadata();
  
        for (Metadata md : metadata) {
            if (md instanceof VorbisComment) {
                VorbisComment comment = (VorbisComment) md;
                
                Assert.assertEquals(15, comment.numComments);
                Assert.assertEquals("reference libFLAC 1.2.1 20070917", new String(comment.vendorString, "US-ASCII"));
                
                Assert.assertEquals("The title", comment.getCommentByName("TITLE")[0]);
                Assert.assertEquals("The artist", comment.getCommentByName("ARTIST")[0]);
                Assert.assertEquals("The album", comment.getCommentByName("ALBUM")[0]);
                Assert.assertEquals("1234", comment.getCommentByName("DATE")[0]);
                Assert.assertEquals("123", comment.getCommentByName("DISCNUMBER")[0]);
                Assert.assertEquals("42", comment.getCommentByName("TRACKNUMBER")[0]);
                Assert.assertEquals("24", comment.getCommentByName("TRACKTOTAL")[0]);
                Assert.assertEquals("The genre", comment.getCommentByName("GENRE")[0]);
                Assert.assertEquals("The comment, with Japanese \u65E5\u672C", comment.getCommentByName("DESCRIPTION")[0]);
                Assert.assertEquals("The comment, with Japanese \u65E5\u672C", comment.getCommentByName("COMMENT")[0]);
                Assert.assertEquals("The original artist", comment.getCommentByName("PERFORMER")[0]);
                Assert.assertEquals("The composer", comment.getCommentByName("COMPOSER")[0]);
                Assert.assertEquals("The copyright", comment.getCommentByName("COPYRIGHT")[0]);
                Assert.assertEquals("http://the.url.com/", comment.getCommentByName("LICENSE")[0]);
                Assert.assertEquals("The encoded by", comment.getCommentByName("ENCODED-BY")[0]);

                return;
            }
        }
        
        Assert.fail("Vorbis comment block not found");
 
    }

    private byte[] readImage(File f) throws IOException {
        byte[] out = new byte[(int) f.length()];

        InputStream in = new BufferedInputStream(new FileInputStream(f));
        int offset = 0;
        int read = 0;
        while (offset < out.length
                && (read = in.read(out, offset, out.length - offset)) >= 0) {
            offset += read;
        }

        in.close();
        return out;

    }
}
