/*
 * Created on Mar 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.util;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ByteSpace {
    public byte[] space;
    public int pos;

    /**
     * 
     */
    public ByteSpace(int maxSpace) {
        space = new byte[maxSpace];
        pos = 0;
    }

}
