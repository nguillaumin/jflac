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
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.kc7bfi.jflac.StreamDecoder;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.PCMDecoder;


public class Player {
	
	public void decode(String inFileName) throws IOException, LineUnavailableException {
		System.out.println("Decode ["+inFileName+"]");
		FileInputStream is = new FileInputStream(inFileName);
		
		StreamDecoder decoder = new StreamDecoder(is);
		decoder.processMetadata();
		
		StreamInfo streamInfo = decoder.getStreamInfo();
		AudioFormat fmt = new AudioFormat(streamInfo.sampleRate,
				streamInfo.bitsPerSample,
				streamInfo.channels,
				true,
				false);
		
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt, AudioSystem.NOT_SPECIFIED);
		SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
		
		line.open(fmt, AudioSystem.NOT_SPECIFIED);
		
		PCMDecoder pcm = new PCMDecoder(decoder.getStreamInfo());
		
		boolean prefill = true;
		boolean eof = false;
		for (int f = 0; !eof; f++) {
			try {
				Frame frame = decoder.getNextFrame();
				if (frame == null) break;
				
				PCMDecoder.Buffer buf = pcm.getFrame(frame, decoder.getChannelData());
				
				line.write(buf.getBuffer(), 0, buf.getLength());
				//System.out.println("wrote "+buf.getLength());
				
				if (prefill) {
					line.start();	
					prefill = false;
				}
			} catch (EOFException e) {
				System.out.println("eof");
				eof = true;
			}
		}
		
		line.close();
	}
	
	public static void main(String[] args) {
		try {
			Player decoder = new Player();
			decoder.decode(args[0]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}
}


