package org.kc7bfi.jflac.sound.spi;

/**
 * libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2001,2002,2003  Josh Coalson
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

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;

import org.kc7bfi.jflac.PCMProcessor;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteSpace;

/**
 * Converts an Flac bitstream into a PCM 16bits/sample audio stream.
 * 
 * @author Marc Gimpel, Wimba S.A. (marc@wimba.com)
 * @version $Revision: 1.4 $
 */
public class Flac2PcmAudioInputStream extends RingedAudioInputStream implements PCMProcessor {

    // audio parameters
    /** The sample rate of the audio, in samples per seconds (Hz). */
    //private int sampleRate;

    /** The number of audio channels (1=mono, 2=stereo). */
    //private int channelCount;

    // Flac variables
    /** Array containing the decoded audio samples. */
    //private float[] decodedData;

    /** Array containing the decoded audio samples converted into bytes. */
    //private byte[] outputData;

    /** Flac bit packing and unpacking class. */
    //private Bits bits;

    /** Flac Decoder. */
    private FLACDecoder decoder;
    
    /** StreamInfo MetaData. */
    private StreamInfo streamInfo;
    
    /** Decode thread. */
    private Thread decodeThread = null;

    /** The frame size, in samples. */
    //private int frameSize;

    /** The number of Flac frames that will be put in each packet. */
    //private int framesPerPacket;

    // variables
    /** A unique serial number that identifies the stream. */
    //private int streamSerialNumber;

    /** The number of packets that are in each page. */
    //private int packetsPerOggPage;

    /** The number of packets that have been decoded in the current page. */
    //private int packetCount;

    /** Array containing the sizes of packets in the current page. */
    //private byte[] packetSizes;

    /** Flag to indicate if this is the first time a decode method is called. */
    //private boolean first;

    /**
     * Constructor.
     * 
     * @param in
     *            the underlying input stream.
     * @param format
     *            the target format of this stream's audio data.
     * @param length
     *            the length in sample frames of the data in this stream.
     */
    public Flac2PcmAudioInputStream(InputStream in, AudioFormat format, long length) {
        this(in, format, length, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Constructor.
     * 
     * @param in
     *            the underlying input stream.
     * @param format
     *            the target format of this stream's audio data.
     * @param length
     *            the length in sample frames of the data in this stream.
     * @param size
     *            the buffer size.
     */
    public Flac2PcmAudioInputStream(InputStream in, AudioFormat format, long length, int size) {
        super(in, format, length, size);
        //bits = new Bits();
        //packetSizes = new byte[256];
        //first = true;
    }
    
    protected void fill() throws IOException {
        if (decodeThread == null) initDecoder();
    }

    /**
     * Initialise the Flac Decoder after reading the Header.
     * @exception IOException
     */
    protected void initDecoder() throws IOException {
        decoder = new FLACDecoder(in);
        decoder.addPCMProcessor(this);
        decodeThread = new Thread(new Runnable() {
            public void run() {
                try {
                    decoder.decode();
                    System.out.println("Frames decoded");
                    buffer.setEOF(true);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        decodeThread.start();
    }
    
    /**
     * Process the StreamInfo block.
     * @param streamInfo the StreamInfo block
     * @see org.kc7bfi.jflac.PCMProcessor#processStreamInfo(org.kc7bfi.jflac.metadata.StreamInfo)
     */
    public void processStreamInfo(StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }
    
    /**
     * Process the decoded PCM bytes.
     * @param pcm The decoded PCM data
     * @see org.kc7bfi.jflac.PCMProcessor#processPCM(org.kc7bfi.jflac.util.ByteSpace)
     */
    public void processPCM(ByteSpace pcm) {
        buffer.resize(pcm.pos * 2);
        buffer.put(pcm.space, 0, pcm.pos);
    }
}
