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

public class Unknown extends MetadataBase {
    protected byte[] data;
    
    /**
     * The constructor.
     * @param is                The InputBitStream
     * @param isLast            True if last metadata record
     * @param length            Length of the record
     * @throws IOException      Thrown if error reading from InputBitStream
     */
    public Unknown(InputBitStream is, boolean isLast, int length) throws IOException {
        super(isLast, length);

        if (length > 0) {
            data = new byte[length];
            is.readByteBlockAlignedNoCRC(data, length);
        }    
    }
}
