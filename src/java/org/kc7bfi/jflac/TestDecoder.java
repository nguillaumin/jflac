/*
 * Created on Mar 22, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.flac.frame.Frame;
import org.flac.io.WavWriter;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TestDecoder {
    static private String inFileName;
    static private String outFileName;
    
    private StreamDecoder decoder;
    
    public TestDecoder() throws IOException {
        System.out.println("["+inFileName+"]["+outFileName+"]");
        FileInputStream is = new FileInputStream(inFileName);
        FileOutputStream os = new FileOutputStream(outFileName);
        decoder = new StreamDecoder(is);
        //decoder.processUntilEndOfStream();
        decoder.processMetadata();
        WavWriter wav = new WavWriter(os, decoder.getStreamInfo());
        wav.writeHeader();
        boolean eof = false;
        for (int f = 0; !eof; f++) {
            try {
                Frame frame = decoder.getNextFrame();
                if (frame == null) break;
                wav.writeFrame(frame, decoder.getChannelData());
            } catch (EOFException e) {
                eof = true;
            }
        }
    }

    public static void main(String[] args) {
        inFileName = args[0];
        outFileName = args[1];
        
        try {
            TestDecoder decoder = new TestDecoder();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
