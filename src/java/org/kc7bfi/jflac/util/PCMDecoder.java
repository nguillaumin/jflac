package org.kc7bfi.jflac.util;

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

import org.kc7bfi.jflac.ChannelData;
import org.kc7bfi.jflac.Constants;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.metadata.StreamInfo;

public class PCMDecoder {
    static final public int MAX_BLOCK_SIZE = 65535;
    
    private long totalSamples;
    private int channels;
    private int bps;
    private int sampleRate;
    
    private int samplesProcessed = 0;
    private int frameCounter = 0;
    
    public class Buffer {
        private byte[] s8buffer = new byte[MAX_BLOCK_SIZE * Constants.MAX_CHANNELS * 4]; /* WATCHOUT: can be up to 2 megs */
        private int len;
        
        public byte[] getBuffer() {
            return s8buffer;
        }
        
        public int getLength() {
            return len;
        }
    }
    
    private Buffer buf;
    
    
    /**
     * The constructor
     * @param streamInfo    The FLAC stream info
     */
    public PCMDecoder(StreamInfo streamInfo) {
        this.totalSamples = streamInfo.totalSamples;
        this.channels = streamInfo.channels;
        this.bps = streamInfo.bitsPerSample;
        this.sampleRate = streamInfo.sampleRate;
        this.buf = new Buffer();
    }
    
    
    /**
     * Write a WAV frame record
     * @param frame         The FLAC frame
     * @param channelData   The decoded channel data
     * @throws IOException  Thrown if error writing to output channel
     */
    public Buffer getFrame(Frame frame, ChannelData[] channelData) throws IOException {
        boolean isUnsignedSamples = (bps <= 8);
        int wideSamples = frame.header.blockSize;
        int wideSample;
        int sample;
        int channel;
        
        if (wideSamples > 0) {
            samplesProcessed += wideSamples;
            frameCounter++;
            if (bps == 8) {
                if (isUnsignedSamples) {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++) {
                            //System.out.print("("+(int)((byte)(channelData[channel].getOutput()[wideSample] + 0x80))+")");
                            buf.s8buffer[sample++] = (byte) (channelData[channel].getOutput()[wideSample] + 0x80);
                        }
                } else {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++)
                            buf.s8buffer[sample++] = (byte) (channelData[channel].getOutput()[wideSample]);
                }
                buf.len = sample;
            } else if (bps == 16) {
                if (isUnsignedSamples) {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++) {
                            short val = (short) (channelData[channel].getOutput()[wideSample] + 0x8000);
                            buf.s8buffer[sample++] = (byte) (val & 0xff);
                            buf.s8buffer[sample++] = (byte) ((val >> 8) & 0xff);
                        }
                } else {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++) {
                            short val = (short) (channelData[channel].getOutput()[wideSample]);
                            buf.s8buffer[sample++] = (byte) (val & 0xff);
                            buf.s8buffer[sample++] = (byte) ((val >> 8) & 0xff);
                        }
                }
                buf.len = sample;
            } else if (bps == 24) {
                if (isUnsignedSamples) {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++) {
                            int val = (channelData[channel].getOutput()[wideSample] + 0x800000);
                            buf.s8buffer[sample++] = (byte) (val & 0xff);
                            buf.s8buffer[sample++] = (byte) ((val >> 8) & 0xff);
                            buf.s8buffer[sample++] = (byte) ((val >> 16) & 0xff);
                        }
                } else {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++) {
                            int val = (channelData[channel].getOutput()[wideSample]);
                            buf.s8buffer[sample++] = (byte) (val & 0xff);
                            buf.s8buffer[sample++] = (byte) ((val >> 8) & 0xff);
                            buf.s8buffer[sample++] = (byte) ((val >> 16) & 0xff);
                        }
                }
                buf.len = sample;
            }
        }
        
        return buf;
    }
}
