/*
 * Created on Mar 6, 2004
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
public class VorbisComment extends MetadataBase {

    static final private int VORBIS_COMMENT_NUM_COMMENTS_LEN = 32; /* bits */

    protected byte[] vendorString = new byte[0];
    protected int numComments = 0;
    protected VorbisString[] comments;

    public VorbisComment(InputBitStream is, boolean isLast, int length) throws IOException {
        super(isLast, length);

        // read vendor string
        int len = is.readRawIntLittleEndian();
        vendorString = new byte[len];
        is.readByteBlockAlignedNoCRC(vendorString, vendorString.length);

        // read comments
        numComments = is.readRawIntLittleEndian();
        if (numComments > 0) comments = new VorbisString[numComments];
        for (int i = 0; i < numComments; i++) {
            comments[i] = new VorbisString(is);
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer("VorbisComment (count="+numComments+")");
        for (int i = 0; i < numComments; i++) {
            sb.append("\n\t"+comments[i].toString());
        }

        return sb.toString();
        
    }
}
