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
import java.io.IOException;

import org.kc7bfi.jflac.FrameListener;
import org.kc7bfi.jflac.StreamDecoder;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.metadata.Metadata;

public class Analyser implements FrameListener {
    private int frameNum = 0;
    
    public void analyse(String inFileName) throws IOException {
        System.out.println("FLAX Analysis for "+inFileName);
        FileInputStream is = new FileInputStream(inFileName);
        StreamDecoder decoder = new StreamDecoder(is);
        decoder.addFrameListener(this);
        decoder.decode();
   }

    public static void main(String[] args) {
        try {
            Analyser analyser = new Analyser();
            analyser.analyse(args[0]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	/* (non-Javadoc)
	 * @see org.kc7bfi.jflac.FrameListener#processMetadata(org.kc7bfi.jflac.metadata.MetadataBase)
	 */
	public void processMetadata(Metadata metadata) {
        System.out.println(metadata.toString());
	}

	/* (non-Javadoc)
	 * @see org.kc7bfi.jflac.FrameListener#processFrame(org.kc7bfi.jflac.frame.Frame)
	 */
	public void processFrame(Frame frame) {
        frameNum++;
        System.out.println(frameNum + " " +frame.toString());
	}
}
