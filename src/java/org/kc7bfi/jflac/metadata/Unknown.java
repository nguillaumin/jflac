/*
 * Created on Mar 13, 2004
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
public class Unknown extends MetadataBase {
    protected byte[] data = null;
    
    public Unknown(InputBitStream is, boolean isLast, int length) throws IOException {
        super(isLast, length);

        if (length > 0) {
            data = new byte[length];
            is.readByteBlockAlignedNoCRC(data, length);
        }    
    }
}
