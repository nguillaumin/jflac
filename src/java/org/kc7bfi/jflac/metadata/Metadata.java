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

public abstract class Metadata {

    /** true if this metadata block is the last, else false */
    protected boolean isLast;
    /** Length, in bytes, of the block data as it appears in the stream. */
    protected int length;
    
    /**
     * The constructor.
     * @param isLast            True if last metadata record
     * @param length            Length of the record
     */
    public Metadata(boolean isLast, int length) {
        this.isLast = isLast;
        this.length = length;
    }

    /**
     * Test iof last metatdata record
     * @return True if last metadata record in the chain.
     */
    public boolean isLast() {
        return isLast;
    }
}
