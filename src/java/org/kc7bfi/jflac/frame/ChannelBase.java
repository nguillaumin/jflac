/*
 * Created on Mar 12, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.frame;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class ChannelBase {
    protected Header header;
    public int wastedBits;
 
    protected ChannelBase(Header header, int wastedBits) {
        this.header = header;
        this.wastedBits = wastedBits;
    }
}
