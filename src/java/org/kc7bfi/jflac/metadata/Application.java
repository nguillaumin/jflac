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
