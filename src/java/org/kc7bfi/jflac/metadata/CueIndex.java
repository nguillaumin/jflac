/*
 * Created on Mar 10, 2004
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
public class CueIndex {

    static final private int CUESHEET_INDEX_OFFSET_LEN = 64; // bits
    static final private int CUESHEET_INDEX_NUMBER_LEN = 8; // bits
    static final private int CUESHEET_INDEX_RESERVED_LEN = 3 * 8; // bits

    protected long offset; // Offset in samples, relative to the track offset, of the index point.
    protected byte number; // The index point number.
    
    public CueIndex(InputBitStream is) throws IOException {
        offset = is.readRawLong(CUESHEET_INDEX_OFFSET_LEN);
        number = (byte) is.readRawUInt(CUESHEET_INDEX_NUMBER_LEN);
        is.skipBitsNoCRC(CUESHEET_INDEX_RESERVED_LEN);
    }
}
