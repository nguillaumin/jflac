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
public class Footer {
    /** CRC-16 (polynomial = x^16 + x^15 + x^2 + x^0, initialized with
     * 0) of the bytes before the crc, back to and including the frame header
     * sync code.
     */
    protected short crc;
}
