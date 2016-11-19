package org.jflac.apps;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Decodes FLAC files with the official decoder, then jFLAC and compare outputs
 * 
 * @author nicolas+github@guillaumin.me
 *
 */
@RunWith(Parameterized.class)
public class DecoderComparisonTest {

    /** System property to override the FLAC binary location */
    private static final String SYSPROP_OVERRIDE_BINARY = "jflac.test.flac.binary";
    
    /** Working folder for the test */
    private static final File OUTDIR = new File("target/test-output/decoder-comparison");
    
    /** Official FLAC binary */
    private static final File FLAC_BINARY = getFlacBinary();

    /** Input FLAC file */
    @Parameter
    public File input;

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        File[] files = new File("../jflac-codec/src/test/resources/testdata").listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".flac");
            }
        });

        List<Object[]> out = new ArrayList<Object[]>();
        for (File f : files) {
            out.add(new Object[] { f });
        }

        return out;
    }

    @Before
    public void before() throws IOException {
        Assume.assumeNotNull(FLAC_BINARY);
        OUTDIR.mkdirs();
        FileUtils.cleanDirectory(OUTDIR);
    }

    @Test
    public void test() throws Exception {
        File flacOut = new File(OUTDIR, input.getName() + ".official.wav");
        Assert.assertFalse(
            "Officially decoded file " + flacOut.getAbsolutePath() + " should not exist prior to the test",
            flacOut.exists());

        String[] cmd = new String[] {
                        FLAC_BINARY.getAbsolutePath(),
                        "--output-name=" + flacOut.getAbsolutePath(),
                        "--decode", input.getAbsolutePath()
        };

        Assert.assertEquals("FLAC decoder exit code", 0, Runtime.getRuntime().exec(cmd).waitFor());
        Assert.assertTrue("File should have been decoded by the official decoder", flacOut.exists());

        File jFlacOut = new File(OUTDIR, input.getName() + ".jflac.wav");
        Assert.assertFalse("jFLAC decoded file should not exist prior to the test", jFlacOut.exists());
        new Decoder().decode(input.getAbsolutePath(), jFlacOut.getAbsolutePath());
        Assert.assertTrue("jFLAC decoded file should have been decoded", jFlacOut.exists());

        Assert.assertArrayEquals("Decoded files should be identical",
            FileUtils.readFileToByteArray(flacOut),
            FileUtils.readFileToByteArray(jFlacOut));
    }
    
    /**
     * Attempt to locate the official FLAC binary either from a provided system
     * property or by looking in the PATH environment variable.
     * 
     * @return FLAC binary, or null if not found
     */
    public static File getFlacBinary() {
        String binaryOverride = System.getProperty(SYSPROP_OVERRIDE_BINARY);
        if (binaryOverride != null && new File(binaryOverride).exists()) {
            File binary = new File(binaryOverride);
            System.out.println("Using provided FLAC binary: " + binary.getAbsolutePath());
            return binary;
        }
        
        String ext = "";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            ext = ".exe";
        }
        
        String path = System.getenv("PATH");
        for (String s: path.split(File.pathSeparator)) {
            File binary = new File(s, "flac" + ext);
            if (binary.exists()) {
                System.out.println("Found FLAC binary: " + binary.getAbsolutePath());
                return binary;
            }
        }
        
        System.out.println("Unable to locate FLAC binary. Ensure it's in your PATH, or set "
            + SYSPROP_OVERRIDE_BINARY + " (e.g. -D" + SYSPROP_OVERRIDE_BINARY + "=C:\\Path\\To\\flac.exe)");
        
        return null;
    }

}
