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

/**
 * SeekTable Metadata block.
 * @author kc7bfi
 */
public class SeekTable extends Metadata {
    private static final int SEEKPOINT_LENGTH = 18;

    protected SeekPoint[] points;

    /**
     * The constructor.
     * @param is                The InputBitStream
     * @param length            Length of the record
     * @throws IOException      Thrown if error reading from InputBitStream
     */
    public SeekTable(InputBitStream is, int length) throws IOException {
        int numPoints = length / SEEKPOINT_LENGTH;

        points = new SeekPoint[numPoints];
        for (int i = 0; i < points.length; i++) {
            points[i] = new SeekPoint(is);
        }
        length -= (length * SEEKPOINT_LENGTH);
        
        // if there is a partial point left, skip over it
        if (length > 0) is.readByteBlockAlignedNoCRC(null, length);
    }
    
    /**
     * Constructor.
     * @param points    Seek Points
     */
    public SeekTable(SeekPoint[] points) {
        this.points = points;
    }
}
