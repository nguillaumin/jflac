/*
 * Created on Mar 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac;

import org.kc7bfi.jflac.frame.*;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ChannelData {
    public int[] output = null;
    public int[] residual = null;
    public EntropyPartitionedRiceContents partitionedRiceContents = null;

    public ChannelData(int size) {
        output = new int[size];
        residual = new int[size];
        partitionedRiceContents = new EntropyPartitionedRiceContents();
    }

}
