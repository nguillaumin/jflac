/*
 * Created on Mar 12, 2004
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
public class VorbisString {
    static final private int VORBIS_COMMENT_ENTRY_LENGTH_LEN = 32; /* bits */

    protected byte[] entry = null;

    public VorbisString(InputBitStream is) throws IOException {
        int elen = is.readRawIntLittleEndian();
        if (elen == 0) return;
        entry = new byte[elen];
        is.readByteBlockAlignedNoCRC(entry, entry.length);
    }
    
    public String toString() {
        return new String(entry);
    }
}
