package org.kc7bfi.jflac.apps;

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

import org.kc7bfi.jflac.StreamDecoder;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.util.WavWriter;

public class Decoder {
    
    public void decode(String inFileName, String outFileName) throws IOException {
        System.out.println("Decode ["+inFileName+"]["+outFileName+"]");
        FileInputStream is = new FileInputStream(inFileName);
        FileOutputStream os = new FileOutputStream(outFileName);
        StreamDecoder decoder = new StreamDecoder(is);
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
        try {
            Decoder decoder = new Decoder();
            decoder.decode(args[0], args[1]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
