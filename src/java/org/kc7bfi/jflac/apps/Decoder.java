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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.kc7bfi.jflac.PCMProcessor;
import org.kc7bfi.jflac.StreamDecoder;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteSpace;
import org.kc7bfi.jflac.util.WavWriter;

public class Decoder implements PCMProcessor {
	private WavWriter wav;
    
    public void decode(String inFileName, String outFileName) throws IOException {
        System.out.println("Decode ["+inFileName+"]["+outFileName+"]");
        FileInputStream is = new FileInputStream(inFileName);
        FileOutputStream os = new FileOutputStream(outFileName);
        wav = new WavWriter(os);
        StreamDecoder decoder = new StreamDecoder(is);
        decoder.addPCMProcessor(this);
        decoder.decode();
    }

	/* (non-Javadoc)
	 * @see org.kc7bfi.jflac.PCMProcessor#processStreamInfo(org.kc7bfi.jflac.metadata.StreamInfo)
	 */
	public void processStreamInfo(StreamInfo info) {
        try {
        	System.out.println("Write WAV header " + info);
			wav.writeHeader(info);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.kc7bfi.jflac.PCMProcessor#processPCM(org.kc7bfi.jflac.util.ByteSpace)
	 */
	public void processPCM(ByteSpace pcm) {
		try {
			System.out.println("Write PCM");
			wav.writePCM(pcm);
		} catch (IOException e) {
			e.printStackTrace();
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
