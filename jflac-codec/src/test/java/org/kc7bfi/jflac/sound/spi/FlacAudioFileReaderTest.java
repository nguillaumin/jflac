/*
 * Copyright 2011 The jFLAC Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kc7bfi.jflac.sound.spi;

import junit.framework.TestCase;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;

/**
 * FlacAudioFileReaderTest.
 * <p/>
 * Date: 4/27/11
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FlacAudioFileReaderTest extends TestCase {

    /**
     * Open simple test file and get stream.
     *
     * @throws UnsupportedAudioFileException
     * @throws IOException
     */
    public void testGetAudioInputStreamWithFlacFile() throws UnsupportedAudioFileException, IOException {
        final FlacAudioFileReader flacAudioFileReader = new FlacAudioFileReader();
        final AudioInputStream stream = flacAudioFileReader.getAudioInputStream(getFlacTestFile("cymbals.flac"));
        assertNotNull(stream);
    }

    /**
     * Open buffered (supporting mark()) inputstream and get audio stream.
     *
     * @throws UnsupportedAudioFileException
     * @throws IOException
     */
    public void testGetAudioInputStreamWithBufferedFlacStream() throws UnsupportedAudioFileException, IOException {
        final FlacAudioFileReader flacAudioFileReader = new FlacAudioFileReader();
        final File flacTestFile = getFlacTestFile("cymbals.flac");

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(flacTestFile));
            assertTrue("For this test the stream MUST support mark()", in.markSupported());
            final AudioInputStream stream = flacAudioFileReader.getAudioInputStream(in);
            assertNotNull(stream);
            final AudioFormat format = stream.getFormat();
            assertEquals(44100f, format.getSampleRate());
            assertEquals(16, format.getSampleSizeInBits());
            assertEquals(2, format.getChannels());
            assertEquals("FLAC", format.getEncoding().toString());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Open unbuffered inputstream, provoke IOException, because we cannot mark and reset.
     *
     * @throws java.io.IOException
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     */
    public void testGetAudioInputStreamWithUnbufferedFlacStream() throws IOException, UnsupportedAudioFileException {
        final FlacAudioFileReader flacAudioFileReader = new FlacAudioFileReader();
        final File flacTestFile = getFlacTestFile("cymbals.flac");

        InputStream in = null;
        try {
            in = new FileInputStream(flacTestFile);
            assertFalse("For this test the stream MUST NOT support mark()", in.markSupported());
            flacAudioFileReader.getAudioInputStream(in);
            fail("Expected an IOException, because the stream didn't support mark. See AudioSystem#getAudioInputStream(InputStream stream) javadocs for contract");
        } catch (IOException e) {
            // expected this
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Open buffered (supporting mark()) inputstream and get format.
     *
     * @throws UnsupportedAudioFileException
     * @throws IOException
     */
    public void testGetAudioFileFormatWithBufferedFlacStream() throws UnsupportedAudioFileException, IOException {
        final FlacAudioFileReader flacAudioFileReader = new FlacAudioFileReader();
        final File flacTestFile = getFlacTestFile("cymbals.flac");

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(flacTestFile));
            assertTrue("For this test the stream MUST support mark()", in.markSupported());
            final AudioFileFormat fileFormat = flacAudioFileReader.getAudioFileFormat(in);
            assertNotNull(fileFormat);
            final AudioFormat format = fileFormat.getFormat();
            assertEquals(44100f, format.getSampleRate());
            assertEquals(16, format.getSampleSizeInBits());
            assertEquals(2, format.getChannels());
            assertEquals("FLAC", format.getEncoding().toString());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Open unbuffered inputstream, provoke IOException, because we cannot mark and reset.
     *
     * @throws java.io.IOException
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     */
    public void testGetAudioFileFormatWithUnbufferedFlacStream() throws IOException, UnsupportedAudioFileException {
        final FlacAudioFileReader flacAudioFileReader = new FlacAudioFileReader();
        final File flacTestFile = getFlacTestFile("cymbals.flac");

        InputStream in = null;
        try {
            in = new FileInputStream(flacTestFile);
            assertFalse("For this test the stream MUST NOT support mark()", in.markSupported());
            flacAudioFileReader.getAudioFileFormat(in);
            fail("Expected an IOException, because the stream didn't support mark. See AudioSystem#getAudioFileFormat(InputStream stream) javadocs for contract");
        } catch (IOException e) {
            // expected this
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Open simple test file and get file format.
     *
     * @throws UnsupportedAudioFileException
     * @throws IOException
     */
    public void testGetAudioFileFormatWithFlacFile() throws UnsupportedAudioFileException, IOException {
        final FlacAudioFileReader flacAudioFileReader = new FlacAudioFileReader();
        final AudioFileFormat audioFileFormat = flacAudioFileReader.getAudioFileFormat(getFlacTestFile("cymbals.flac"));
        assertNotNull(audioFileFormat);
        assertEquals("flac", audioFileFormat.getType().getExtension());
        assertEquals(new Long(9338775), audioFileFormat.getProperty("duration"));
        assertEquals(411840, audioFileFormat.getFrameLength());
        final AudioFormat format = audioFileFormat.getFormat();
        assertEquals(44100f, format.getSampleRate());
        assertEquals(16, format.getSampleSizeInBits());
        assertEquals(2, format.getChannels());
        assertEquals("FLAC", format.getEncoding().toString());
    }

    /**
     * Provoke UnsupportedAudioFileException when trying to obtain stream.
     *
     * @throws IOException
     */
    public void testGetAudioInputStreamWithUnsupportedFile() throws IOException {
        final FlacAudioFileReader flacAudioFileReader = new FlacAudioFileReader();
        final File file = File.createTempFile("flacTest", ".wav");
        final OutputStream out = new FileOutputStream(file);
        out.write(new byte[2048]);
        out.close();
        try {
            flacAudioFileReader.getAudioInputStream(file);
            fail("Expected UnsupportedAudioFileException");
        } catch (UnsupportedAudioFileException e) {
            // expected this
        } finally {
            file.delete();
        }
    }

    /**
     * Provoke UnsupportedAudioFileException when trying to obtain audio file format.
     *
     * @throws IOException
     */
    public void testGetAudioFileFormatWithUnsupportedFile() throws IOException {
        final FlacAudioFileReader flacAudioFileReader = new FlacAudioFileReader();
        final File file = File.createTempFile("flacTest", ".wav");
        final OutputStream out = new FileOutputStream(file);
        out.write(new byte[2048]);
        out.close();
        try {
            flacAudioFileReader.getAudioInputStream(file);
            fail("Expected UnsupportedAudioFileException");
        } catch (UnsupportedAudioFileException e) {
            // expected this
        } finally {
            file.delete();
        }
    }

    /**
     * Extracts a file from the classpath (located in /testdata/).
     *
     *
     * @param name name of the file.
     * @return file object
     * @throws java.io.IOException
     */
    private File getFlacTestFile(final String name) throws IOException {
        final InputStream in = getClass().getResourceAsStream("/testdata/" + name);
        if (in == null) return null;
        final File tempFile = File.createTempFile("flacTest", ".flac");
        tempFile.deleteOnExit();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tempFile);
            int justRead;
            byte[] buf = new byte[1024 * 8];
            while ((justRead = in.read(buf)) > 0) {
                out.write(buf, 0, justRead);
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return tempFile;
    }

}
