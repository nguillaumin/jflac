package org.jflac.apps;

import org.apache.commons.io.FileUtils;
import org.jflac.util.WavWriter;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Decodes FLAC files with the official decoder, then jFLAC and compare outputs
 * 
 * @author nicolas+github@guillaumin.me
 *
 */
@RunWith(Parameterized.class)
public class DecoderStreamComparisonTest {

    /** System property to override the FLAC binary location */
    private static final String SYSPROP_OVERRIDE_BINARY = "jflac.test.flac.binary";
    
    /** Working folder for the test */
    private static final File OUTDIR = new File("target/test-output/decoder-comparison-stream");
    
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
        RandomAccessFile randomAccessFile = new RandomAccessFile(jFlacOut, "rw");
        WavWriter wavWriter = new WavWriter(randomAccessFile);
        wavWriter.writeHeader();

        new Decoder().decode(input.getAbsolutePath(), jFlacOut.getAbsolutePath());
        File soundFile = input;
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        AudioFormat audioFormat = audioInputStream.getFormat();
        if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            AudioFormat newFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    audioFormat.getSampleRate(),
                    (audioFormat.getSampleSizeInBits() > 0) ? audioFormat.getSampleSizeInBits() : 16,
                    audioFormat.getChannels(),
                    (audioFormat.getSampleSizeInBits() > 0) ? audioFormat.getChannels() * audioFormat.getSampleSizeInBits() / 8 : audioFormat.getChannels() * 2,
                    audioFormat.getSampleRate(),
                    false);
            System.out.println("Converting audio format to " + newFormat);
            AudioInputStream newStream = AudioSystem.getAudioInputStream(newFormat, audioInputStream);
            audioFormat = newFormat;
            audioInputStream = newStream;
        }

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        Assert.assertTrue("Play.playAudioStream does not handle this type of audio on this system.", AudioSystem.isLineSupported(info));

        SourceDataLine dataLine = (SourceDataLine) AudioSystem.getLine(info);
        dataLine.open(audioFormat);

        dataLine.start();

        int bufferSize = (int) audioFormat.getSampleRate() * audioFormat.getFrameSize();
        byte [] buffer = new byte[ bufferSize ];
        int bytesRead = 0;
        while (bytesRead >= 0) {
            bytesRead = audioInputStream.read(buffer, 0, buffer.length);
            if (bytesRead >= 0) {
                // System.out.println("Play.playAudioStream bytes read=" + bytesRead +
                //    ", frame size=" + audioFormat.getFrameSize() + ", frames read=" + bytesRead / audioFormat.getFrameSize());
                // Odd sized sounds throw an exception if we don't write the same amount.
                randomAccessFile.write(buffer, 0, bytesRead);
            }
        } // while
        dataLine.drain();
        dataLine.close();
        randomAccessFile.close();

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
    private static File getFlacBinary() {
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
