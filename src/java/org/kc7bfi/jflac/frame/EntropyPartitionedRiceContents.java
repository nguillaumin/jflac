/*
 * Created on Mar 10, 2004
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
public class EntropyPartitionedRiceContents {

    protected int[] parameters = null; // The Rice parameters for each context.
    protected int[] rawBits = null; // Widths for escape-coded partitions.

    /** The capacity of the \a parameters and \a raw_bits arrays
     * specified as an order, i.e. the number of array elements
     * allocated is 2 ^ \a capacity_by_order.
     */
    protected int capacityByOrder = 0;
    
    public void ensureSize(int max_partition_order) {
        if (capacityByOrder >= max_partition_order) return;
        parameters = new int[(1 << max_partition_order)];
        rawBits = new int[(1 << max_partition_order)]; 
        capacityByOrder = max_partition_order;
    }
}
