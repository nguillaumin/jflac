package org.kc7bfi.jflac.metadata;

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

import org.kc7bfi.jflac.util.InputBitStream;

public class StreamInfo extends MetadataBase {

    private static final int STREAMINFO_MIN_BLOCK_SIZE_LEN = 16; // bits
    private static final int STREAMINFO_MAX_BLOCK_SIZE_LEN = 16; // bits
    private static final int STREAMINFO_MIN_FRAME_SIZE_LEN = 24; // bits
    private static final int STREAMINFO_MAX_FRAME_SIZE_LEN = 24; // bits
    private static final int STREAMINFO_SAMPLE_RATE_LEN = 20; // bits
    private static final int STREAMINFO_CHANNELS_LEN = 3; // bits
    private static final int STREAMINFO_BITS_PER_SAMPLE_LEN = 5; // bits
    private static final int STREAMINFO_TOTAL_SAMPLES_LEN = 36; // bits
    private static final int STREAMINFO_MD5SUM_LEN = 128; // bits

    protected byte[] md5sum = new byte[16];

    public int minBlockSize;
    public int maxBlockSize;
    public int minFrameSize;
    public int maxFrameSize;
    public int sampleRate;
    public int channels;
    public int bitsPerSample;
    public long totalSamples;
    
    /**
     * The constructor.
     * @param is                The InputBitStream
     * @param isLast            True if last metadata record
     * @param length            Length of the record
     * @throws IOException      Thrown if error reading from InputBitStream
     */
    public StreamInfo(InputBitStream is, boolean isLast, int length) throws IOException {
        super(isLast, length);
        
        int usedBits = 0;

        minBlockSize = is.readRawUInt(STREAMINFO_MIN_BLOCK_SIZE_LEN);
        usedBits += STREAMINFO_MIN_BLOCK_SIZE_LEN;

        maxBlockSize = is.readRawUInt(STREAMINFO_MAX_BLOCK_SIZE_LEN);
        usedBits += STREAMINFO_MAX_BLOCK_SIZE_LEN;

        minFrameSize = is.readRawUInt(STREAMINFO_MIN_FRAME_SIZE_LEN);
        usedBits += STREAMINFO_MIN_FRAME_SIZE_LEN;

        maxFrameSize = is.readRawUInt(STREAMINFO_MAX_FRAME_SIZE_LEN);
        usedBits += STREAMINFO_MAX_FRAME_SIZE_LEN;

        sampleRate = is.readRawUInt(STREAMINFO_SAMPLE_RATE_LEN);
        usedBits += STREAMINFO_SAMPLE_RATE_LEN;

        channels = is.readRawUInt(STREAMINFO_CHANNELS_LEN) + 1;
        usedBits += STREAMINFO_CHANNELS_LEN;

        bitsPerSample = is.readRawUInt(STREAMINFO_BITS_PER_SAMPLE_LEN) + 1;
        usedBits += STREAMINFO_BITS_PER_SAMPLE_LEN;

        totalSamples = is.readRawLong(STREAMINFO_TOTAL_SAMPLES_LEN);
        usedBits += STREAMINFO_TOTAL_SAMPLES_LEN;

        is.readByteBlockAlignedNoCRC(md5sum, STREAMINFO_MD5SUM_LEN / 8);
        usedBits += 16 * 8;

        // skip the rest of the block
        length -= (usedBits / 8);
        is.readByteBlockAlignedNoCRC(null, length);
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "StreamInfo:" 
            + " BlockSize=" + minBlockSize + "-" + maxBlockSize
            + " FrameSize" + minFrameSize + "-" + maxFrameSize
            + " SampelRate=" + sampleRate
            + " Channels=" + channels
            + " BPS=" + bitsPerSample
            + " TotalSamples=" + totalSamples;
    }
}
