package org.kc7bfi.jflac;

/* libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2000,2001,2002,2003  Josh Coalson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 */

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.metadata.MetadataBase;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.WavWriter;

public class TestDecoder {
    static private String inFileName;
    static private String outFileName;
    
    private StreamDecoder decoder;
    
    public TestDecoder() {
    }
    
    public void analyse() throws IOException {
        System.out.println("["+inFileName+"]["+outFileName+"]");
        FileInputStream is = new FileInputStream(inFileName);
        FileOutputStream os = new FileOutputStream(outFileName);
        decoder = new StreamDecoder(is);
        
        StreamInfo info = decoder.readStreamInfo();
        System.out.println(info.toString());
        MetadataBase metadata;
        do {
            metadata = decoder.readNextMetadata();
            System.out.println(metadata.toString());
        } while (!metadata.isLast());
   }
    
    public void decode() throws IOException {
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
            TestDecoder decoder1 = new TestDecoder();
            decoder1.analyse();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
