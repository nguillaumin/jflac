/*
 * Created on Mar 7, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.metadata;

import java.io.IOException;

import org.kc7bfi.jflac.io.InputBitStream;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SeekPoint {

    static final private int SEEKPOINT_SAMPLE_NUMBER_LEN = 64; // bits
    static final private int SEEKPOINT_STREAM_OFFSET_LEN = 64; // bits
    static final private int SEEKPOINT_FRAME_SAMPLES_LEN = 16; // bits

    protected long sampleNumber; // The sample number of the target frame.
    protected long streamOffset; // The offset, in bytes, of the target frame with respect to beginning of the first frame.
    protected int frameSamples; // The number of samples in the target frame.
    
    public SeekPoint(InputBitStream is) throws IOException {
        sampleNumber = is.readRawLong(SEEKPOINT_SAMPLE_NUMBER_LEN);
        streamOffset = is.readRawLong(SEEKPOINT_STREAM_OFFSET_LEN);
        frameSamples = is.readRawUInt(SEEKPOINT_FRAME_SAMPLES_LEN);
    }

    /* used as the sort predicate for qsort() */
    int compare(SeekPoint r) {
        /* we don't just 'return l->sample_number - r->sample_number' since the result (int64) might overflow an 'int' */
        if (sampleNumber == r.sampleNumber) return 0;
        if (sampleNumber < r.sampleNumber) return -1;
        return 1;
    }
}
