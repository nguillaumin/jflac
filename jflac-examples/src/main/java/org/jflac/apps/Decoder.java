package org.jflac.apps;

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

import org.jflac.PCMProcessor;
import org.jflac.FLACDecoder;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;
import org.jflac.util.WavWriter;

/**
 * Decode FLAC file to WAV file application.
 * @author kc7bfi
 */
public class Decoder implements PCMProcessor {
    private WavWriter wav;
    
    /**
     * Decode a FLAC file to a WAV file.
     * @param inFileName    The input FLAC file name
     * @param outFileName   The output WAV file name
     * @throws IOException  Thrown if error reading or writing files
     */
    public void decode(String inFileName, String outFileName) throws IOException {
        System.out.println("Decode [" + inFileName + "][" + outFileName + "]");
        FileInputStream is = new FileInputStream(inFileName);
        FileOutputStream os = new FileOutputStream(outFileName);
        wav = new WavWriter(os);
        FLACDecoder decoder = new FLACDecoder(is);
        decoder.addPCMProcessor(this);
        decoder.decode();
    }
    
    /**
     * Process the StreamInfo block.
     * @param info the StreamInfo block
     * @see org.jflac.PCMProcessor#processStreamInfo(org.jflac.metadata.StreamInfo)
     */
    public void processStreamInfo(StreamInfo info) {
        try {
            wav.writeHeader(info);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Process the decoded PCM bytes.
     * @param pcm The decoded PCM data
     * @see org.jflac.PCMProcessor#processPCM(org.jflac.util.ByteSpace)
     */
    public void processPCM(ByteData pcm) {
        try {
            wav.writePCM(pcm);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Main routine.
     * <p>args[0] is the input file name
     * <p>args[1] is the output file name
     * @param args  Command line arguments
     */
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
