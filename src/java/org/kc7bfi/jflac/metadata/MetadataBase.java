/*
 * Created on Mar 12, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.metadata;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class MetadataBase {

    protected boolean isLast; // true if this metadata block is the last, else false
    protected int length; // Length, in bytes, of the block data as it appears in the stream.
    
    public MetadataBase(boolean isLast, int length) {
        this.isLast = isLast;
        this.length = length;
    }

}
