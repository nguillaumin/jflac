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

public class VorbisString {
    static final private int VORBIS_COMMENT_ENTRY_LENGTH_LEN = 32; // bits

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
