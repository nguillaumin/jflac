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

import org.kc7bfi.jflac.io.InputBitStream;

public class Application extends MetadataBase {

    static final private int APPLICATION_ID_LEN = 32; // bits

    protected byte[] id = new byte[4];
    protected byte[] data = null;

    public Application(InputBitStream is, boolean isLast, int length) throws IOException {
        super(isLast, length);

        is.readByteBlockAlignedNoCRC(id, APPLICATION_ID_LEN / 8);
        length -= APPLICATION_ID_LEN / 8;

        if (length > 0) {
            data = new byte[length];
            is.readByteBlockAlignedNoCRC(data, length);
        }
    }
}
