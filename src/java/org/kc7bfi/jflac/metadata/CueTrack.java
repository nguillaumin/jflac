/*
 * Created on Mar 10, 2004
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
public class CueTrack {

    static final private int CUESHEET_TRACK_OFFSET_LEN = 64; // bits
    static final private int CUESHEET_TRACK_NUMBER_LEN = 8; // bits
    static final private int CUESHEET_TRACK_ISRC_LEN = 12 * 8; // bits
    static final private int CUESHEET_TRACK_TYPE_LEN = 1; // bit
    static final private int CUESHEET_TRACK_PRE_EMPHASIS_LEN = 1; // bit
    static final private int CUESHEET_TRACK_RESERVED_LEN = 6 + 13 * 8; // bits
    static final private int CUESHEET_TRACK_NUM_INDICES_LEN = 8; // bits

    protected long offset; // Track offset in samples, relative to the beginning of the FLAC audio stream.
    protected byte number; // The track number.
    protected byte[] isrc = new byte[13]; // Track ISRC.  This is a 12-digit alphanumeric code plus a trailing '\0'
    protected int type; // The track type: 0 for audio, 1 for non-audio.
    protected int preEmphasis; // The pre-emphasis flag: 0 for no pre-emphasis, 1 for pre-emphasis.
    protected byte numIndices; // The number of track index points.
    protected CueIndex[] indices = null; // NULL if num_indices == 0, else pointer to array of index points.

    public CueTrack(InputBitStream is) throws IOException {
        offset = is.readRawLong(CUESHEET_TRACK_OFFSET_LEN);
        number = (byte)is.readRawUInt(CUESHEET_TRACK_NUMBER_LEN);
        is.readByteBlockAlignedNoCRC(isrc, CUESHEET_TRACK_ISRC_LEN / 8);
        type = is.readRawUInt(CUESHEET_TRACK_TYPE_LEN);
        preEmphasis = is.readRawUInt(CUESHEET_TRACK_PRE_EMPHASIS_LEN);
        is.skipBitsNoCRC(CUESHEET_TRACK_RESERVED_LEN);
        if (numIndices > 0) {
            indices = new CueIndex[numIndices];
            for (int j = 0; j < numIndices; j++) {
                indices[j] = new CueIndex(is);
            }
        }
        
    }
}
