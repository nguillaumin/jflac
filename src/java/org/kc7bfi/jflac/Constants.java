package org.kc7bfi.jflac;


/* libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2000,2001,2002,2003  Josh Coalson
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

public class Constants {
    /* VERSION should come from configure */
    static final public String VERSION_STRING = "1.0";
    /* yet one more hack because of MSVC6: */
    static final public String VENDOR_STRING = "reference libFLAC 1.1.0 20030126";

    static final public byte[] STREAM_SYNC_STRING = new byte[] { (byte)'f', (byte)'L', (byte)'a', (byte)'C' };
    static final public int STREAM_SYNC = 0x664C6143;
    static final public int STREAM_SYNC_LEN = 32; /* bits */;
    
    static final public int MAX_CHANNELS = 8;
    static final public int MAX_BLOCK_SIZE = 65535;

    static final public int STREAM_METADATA_IS_LAST_LEN = 1; /* bits */
    static final public int STREAM_METADATA_TYPE_LEN = 7; /* bits */
    static final public int STREAM_METADATA_LENGTH_LEN = 24; /* bits */

    static final public int FRAME_HEADER_SYNC = 0x3ffe;
    static final public int FRAME_HEADER_SYNC_LEN = 14; /* bits */
    static final public int FRAME_HEADER_RESERVED_LEN = 2; /* bits */
    static final public int FRAME_HEADER_BLOCK_SIZE_LEN = 4; /* bits */
    static final public int FRAME_HEADER_SAMPLE_RATE_LEN = 4; /* bits */
    static final public int FRAME_HEADER_CHANNEL_ASSIGNMENT_LEN = 4; /* bits */
    static final public int FRAME_HEADER_BITS_PER_SAMPLE_LEN = 3; /* bits */
    static final public int FRAME_HEADER_ZERO_PAD_LEN = 1; /* bits */
    static final public int FRAME_HEADER_CRC_LEN = 8; /* bits */

    static final public int FRAME_FOOTER_CRC_LEN = 16; /* bits */

    static final public int ENTROPY_CODING_METHOD_PARTITIONED_RICE = 0;
    static final public int ENTROPY_CODING_METHOD_TYPE_LEN = 2; /* bits */
    static final public int ENTROPY_CODING_METHOD_PARTITIONED_RICE_ORDER_LEN = 4; /* bits */
    static final public int ENTROPY_CODING_METHOD_PARTITIONED_RICE_PARAMETER_LEN = 4; /* bits */
    static final public int ENTROPY_CODING_METHOD_PARTITIONED_RICE_RAW_LEN = 5; /* bits */

    static final public int ENTROPY_CODING_METHOD_PARTITIONED_RICE_ESCAPE_PARAMETER = 15;
    /* == (1<<ENTROPY_CODING_METHOD_PARTITIONED_RICE_PARAMETER_LEN)-1 */

    static final public String[] EntropyCodingMethodTypeString = new String[] { "PARTITIONED_RICE" };

    static final public int SUBFRAME_LPC_QLP_COEFF_PRECISION_LEN = 4; /* bits */
    static final public int SUBFRAME_LPC_QLP_SHIFT_LEN = 5; /* bits */

    static final public int SUBFRAME_ZERO_PAD_LEN = 1; /* bits */
    static final public int SUBFRAME_TYPE_LEN = 6; /* bits */
    static final public int SUBFRAME_WASTED_BITS_FLAG_LEN = 1; /* bits */

    static final public int SUBFRAME_TYPE_CONSTANT_BYTE_ALIGNED_MASK = 0x00;
    static final public int SUBFRAME_TYPE_VERBATIM_BYTE_ALIGNED_MASK = 0x02;
    static final public int SUBFRAME_TYPE_FIXED_BYTE_ALIGNED_MASK = 0x10;
    static final public int SUBFRAME_TYPE_LPC_BYTE_ALIGNED_MASK = 0x40;
    
    static final public int MAX_RICE_PARTITION_ORDER = 15;
    
    // metadata type
    static final public int METADATA_TYPE_STREAMINFO = 0;
    static final public int METADATA_TYPE_PADDING = 1;
    static final public int METADATA_TYPE_APPLICATION = 2;
    static final public int METADATA_TYPE_SEEKTABLE = 3;
    static final public int METADATA_TYPE_VORBIS_COMMENT = 4;
    static final public int METADATA_TYPE_CUESHEET = 5;
    static final public int METADATA_TYPE_UNDEFINED = 6;
    
    // channel assignment
    static final public int CHANNEL_ASSIGNMENT_INDEPENDENT = 0; /**< independent channels */
    static final public int CHANNEL_ASSIGNMENT_LEFT_SIDE = 1; /**< left+side stereo */
    static final public int CHANNEL_ASSIGNMENT_RIGHT_SIDE = 2; /**< right+side stereo */
    static final public int CHANNEL_ASSIGNMENT_MID_SIDE = 3; /**< mid+side stereo */

    static final public String[] SubframeTypeString = new String[] { "CONSTANT", "VERBATIM", "FIXED", "LPC" };

    static final public String[] ChannelAssignmentString = new String[] { "INDEPENDENT", "LEFT_SIDE", "RIGHT_SIDE", "MID_SIDE" };

    static final public String[] FrameNumberTypeString = new String[] 
        { "FRAME_NUMBER_TYPE_FRAME_NUMBER", "FRAME_NUMBER_TYPE_SAMPLE_NUMBER" };

    static final public String[] MetadataTypeString = new String[] 
        { "STREAMINFO", "PADDING", "APPLICATION", "SEEKTABLE", "VORBIS_COMMENT", "CUESHEET" };
        
    static final public int MAX_SAMPLE_RATE = 655350;


        /*
    boolean format_sample_rate_is_valid(int sample_rate) {
        if (sample_rate == 0
            || sample_rate > MAX_SAMPLE_RATE
            || (sample_rate >= (1 << 16) && !(sample_rate % 1000 == 0 || sample_rate % 10 == 0))) {
            return false;
        } else
            return true;
    }
    */

    /*
     * These routines are private to libFLAC
     */
        /*
    int format_get_max_rice_partition_order(int blocksize, int predictor_order) {
        return format_get_max_rice_partition_order_from_blocksize_limited_max_and_predictor_order(
            format_get_max_rice_partition_order_from_blocksize(blocksize),
            blocksize,
            predictor_order);
    }

    int format_get_max_rice_partition_order_from_blocksize(int blocksize) {
        int max_rice_partition_order = 0;
        while ((blocksize & 1) == 0) {
            max_rice_partition_order++;
            blocksize >>= 1;
        }
        return Math.min(MAX_RICE_PARTITION_ORDER, max_rice_partition_order);
    }

    int format_get_max_rice_partition_order_from_blocksize_limited_max_and_predictor_order(
        int limit,
        int blocksize,
        int predictor_order) {
        int max_rice_partition_order = limit;

        while (max_rice_partition_order > 0 && (blocksize >> max_rice_partition_order) <= predictor_order)
            max_rice_partition_order--;

        return max_rice_partition_order;
    }
    */
}