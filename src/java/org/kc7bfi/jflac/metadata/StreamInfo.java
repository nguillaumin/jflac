/*
 * Created on Mar 7, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.metadata;

import java.io.IOException;

import org.flac.io.InputBitStream;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class StreamInfo extends MetadataBase {

    static final private int STREAMINFO_MIN_BLOCK_SIZE_LEN = 16; // bits
    static final private int STREAMINFO_MAX_BLOCK_SIZE_LEN = 16; // bits
    static final private int STREAMINFO_MIN_FRAME_SIZE_LEN = 24; // bits
    static final private int STREAMINFO_MAX_FRAME_SIZE_LEN = 24; // bits
    static final private int STREAMINFO_SAMPLE_RATE_LEN = 20; // bits
    static final private int STREAMINFO_CHANNELS_LEN = 3; // bits
    static final private int STREAMINFO_BITS_PER_SAMPLE_LEN = 5; // bits
    static final private int STREAMINFO_TOTAL_SAMPLES_LEN = 36; // bits
    static final private int STREAMINFO_MD5SUM_LEN = 128; // bits

    protected byte[] md5sum = new byte[16];

    public int minBlockSize;
    public int maxBlockSize;
    public int minFrameSize;
    public int maxFrameSize;
    public int sampleRate;
    public int channels;
    public int bitsPerSample;
    public long totalSamples;
    
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

        is.readByteBlockAlignedNoCRC(md5sum, 16);
        usedBits += 16 * 8;

        /* skip the rest of the block */
        length -= (usedBits / 8);
        is.readByteBlockAlignedNoCRC(null, length);
    }
    
    public String toString() {
        return "StreamInfo:" 
            + " BlockSize="+minBlockSize+"-"+maxBlockSize
            + " FrameSize"+minFrameSize+"-"+maxFrameSize
            + " SampelRate="+sampleRate
            + " Channels="+channels
            + " BPS="+bitsPerSample
            + " TotalSamples="+totalSamples;
    }
}
