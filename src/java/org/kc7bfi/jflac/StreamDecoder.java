package org.kc7bfi.jflac;

import java.io.IOException;
import java.io.InputStream;

import org.flac.frame.BadHeaderException;
import org.flac.frame.ChannelConstant;
import org.flac.frame.ChannelFixed;
import org.flac.frame.ChannelLPC;
import org.flac.frame.ChannelVerbatim;
import org.flac.frame.Frame;
import org.flac.frame.Header;
import org.flac.io.InputBitStream;
import org.flac.metadata.Application;
import org.flac.metadata.CueSheet;
import org.flac.metadata.MetadataBase;
import org.flac.metadata.SeekTable;
import org.flac.metadata.StreamInfo;
import org.flac.metadata.Unknown;
import org.flac.metadata.VorbisComment;
import org.flac.util.CRC16;

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

public class StreamDecoder {

    /***********************************************************************
     *
     * Private static data
     *
     ***********************************************************************/

    static private final byte[] ID3V2_TAG_ = new byte[] { 'I', 'D', '3' };
    static private final int MAX_CHANNELS = 8;

    // errors
    /**< An error in the stream caused the decoder to lose synchronization. */
    static private final int STREAM_DECODER_ERROR_STATUS_LOST_SYNC = 1;

    /**< The decoder encountered a corrupted frame header. */
    static private final int STREAM_DECODER_ERROR_STATUS_BAD_HEADER = 2;

    /**< The frame's data did not match the CRC in the footer. */
    static private final int STREAM_DECODER_ERROR_STATUS_FRAME_CRC_MISMATCH = 3;

    // write callback codes

    static private final int STREAM_DECODER_WRITE_STATUS_CONTINUE = 0;
    /**< The write was OK and decoding can continue. */

    static private final int STREAM_DECODER_WRITE_STATUS_ABORT = 0;
    /**< An unrecoverable error occurred.  The decoder will return from the process call. */

    /***********************************************************************
     *
     * Private class data
     *
     ***********************************************************************/

    private InputBitStream is = null;
    //private OutputStream os = null;
    //private StreamDecoderWriteCallback write_callback;
    //private StreamDecoderMetadataCallback metadata_callback;
    //private StreamDecoderErrorCallback error_callback;
    //private void (* local_lpc_restore_signal) (const int32 residual[], unsigned data_len, const int32 qlp_coeff[], unsigned order, int lp_quantization, int32 data[]);
    //private void (* local_lpc_restore_signal_64bit) (const int32 residual[], unsigned data_len, const int32 qlp_coeff[], unsigned order, int lp_quantization, int32 data[]);
    //private void (* local_lpc_restore_signal_16bit) const int32 residual[], unsigned data_len, const int32 qlp_coeff[], unsigned order, int lp_quantization, int32 data[]);
    //private void * client_data;
    //private InputBitStream input = new InputBitStream();
    private ChannelData[] channelData = new ChannelData[MAX_CHANNELS];
    //private int[][] output = new int[MAX_CHANNELS][];
    //private int[][] residual = new int[MAX_CHANNELS][];
    //private EntropyCodingMethod_PartitionedRiceContents[] partitionedRiceContents = new EntropyCodingMethod_PartitionedRiceContents[MAX_CHANNELS];
    private int outputCapacity = 0;
    private int outputChannels = 0;
    private int lastFrameNumber;
    private long samplesDecoded;
    //private boolean has_stream_info;
    //private boolean has_seek_table;
    StreamInfo streamInfo = null;
    SeekTable seekTable = null;
    //private boolean[] metadata_filter = new boolean[128]; /* MAGIC number 128 == total number of metadata block types == 1 << 7 */
    //private Vector metadata_filter_ids = new Vector();
    //private int metadata_filter_ids_count;
    //private int  metadata_filter_ids_capacity; /* units for both are IDs, not bytes */
    private Frame frame = new Frame();
    //private boolean cached; /* true if there is a byte in lookahead */
    //private CPUInfo cpuinfo;
    private byte[] headerWarmup = new byte[2]; /* contains the sync code and reserved bits */
    //private byte lookahead; /* temp storage when we need to look ahead one byte in the stream */
    private int state;
    private int channels;
    private int channelAssignment;
    private int bitsPerSample;
    private int sampleRate; /* in Hz */
    private int blockSize; /* in samples (per channel) */

    /***********************************************************************
     *
     * Public static class data
     *
     ***********************************************************************/

    static final String[] StreamDecoderStateString =
        new String[] {
            "STREAM_DECODER_SEARCH_FOR_METADATA",
            "STREAM_DECODER_READ_METADATA",
            "STREAM_DECODER_SEARCH_FOR_FRAME_SYNC",
            "STREAM_DECODER_READ_FRAME",
            "STREAM_DECODER_END_OF_STREAM",
            "STREAM_DECODER_ABORTED",
            "STREAM_DECODER_UNPARSEABLE_STREAM",
            "STREAM_DECODER_MEMORY_ALLOCATION_ERROR",
            "STREAM_DECODER_ALREADY_INITIALIZED",
            "STREAM_DECODER_INVALID_CALLBACK",
            "STREAM_DECODER_UNINITIALIZED" };

    private static final String[] StreamDecoderReadStatusString =
        new String[] {
            "STREAM_DECODER_READ_STATUS_CONTINUE",
            "STREAM_DECODER_READ_STATUS_END_OF_STREAM",
            "STREAM_DECODER_READ_STATUS_ABORT" };

    private static final String[] StreamDecoderWriteStatusString =
        new String[] { "STREAM_DECODER_WRITE_STATUS_CONTINUE", "STREAM_DECODER_WRITE_STATUS_ABORT" };

    private static final String[] StreamDecoderErrorStatusString =
        new String[] {
            "STREAM_DECODER_ERROR_STATUS_LOST_SYNC",
            "STREAM_DECODER_ERROR_STATUS_BAD_HEADER",
            "STREAM_DECODER_ERROR_STATUS_FRAME_CRC_MISMATCH" };

    // Decoder states
    static final int STREAM_DECODER_SEARCH_FOR_METADATA = 0;
    static final int STREAM_DECODER_READ_METADATA = 1;
    static final int STREAM_DECODER_SEARCH_FOR_FRAME_SYNC = 2;
    static final int STREAM_DECODER_READ_FRAME = 3;
    static final int STREAM_DECODER_END_OF_STREAM = 4;
    static final int STREAM_DECODER_ABORTED = 5;
    static final int STREAM_DECODER_UNPARSEABLE_STREAM = 6;
    static final int STREAM_DECODER_MEMORY_ALLOCATION_ERROR = 7;
    static final int STREAM_DECODER_ALREADY_INITIALIZED = 8;
    static final int STREAM_DECODER_INVALID_CALLBACK = 9;
    static final int STREAM_DECODER_UNINITIALIZED = 10;

    /***********************************************************************
     *
     * Class constructor/destructor
     *
     ***********************************************************************/
    //public StreamDecoder() {
    //    state = STREAM_DECODER_UNINITIALIZED;
    //    for (int i = 0; i < metadata_filter.length; i++)
    //        metadata_filter[i] = false;
    //    metadata_filter[Format.METADATA_TYPE_STREAMINFO] = true;
    //    metadata_filter_ids.removeAllElements();
    //}

    public StreamDecoder(InputStream is) {
        this.is = new InputBitStream(is);
        //this.os = os;
        
        state = STREAM_DECODER_UNINITIALIZED;
        //for (int i = 0; i < metadata_filter.length; i++)
        //    metadata_filter[i] = true; // DRR false;
        //metadata_filter[Format.METADATA_TYPE_STREAMINFO] = true;
        //metadata_filter_ids.removeAllElements();

        lastFrameNumber = 0;
        samplesDecoded = 0;
        //cached = false;
        
        state = STREAM_DECODER_SEARCH_FOR_METADATA;
    }

    //void delete() {
    //    finish();
    //}

    /***********************************************************************
     *
     * Public class methods
     *
     ***********************************************************************/

/*
    int init() {
        if (state != STREAM_DECODER_UNINITIALIZED)
            return state = STREAM_DECODER_ALREADY_INITIALIZED;

        //if (is == null || write_callback == null || metadata_callback == null || error_callback == null
        //    )
        //    return state = STREAM_DECODER_INVALID_CALLBACK;

        last_frame_number = 0;
        samples_decoded = 0;
        //has_stream_info = false;
        cached = false;

        // get the CPU info and set the function pointers
         //
        //cpu_info(& cpuinfo);
        // first default to the non-asm routines 
        //local_lpc_restore_signal = lpc_restore_signal;
        //local_lpc_restore_signal_64bit = lpc_restore_signal_wide;
        //local_lpc_restore_signal_16bit = lpc_restore_signal;
        // now override with asm where appropriate 
        if (!reset())
            return state = STREAM_DECODER_MEMORY_ALLOCATION_ERROR;

        return state;
    }
    */

    boolean finish() {
        if (state == STREAM_DECODER_UNINITIALIZED)
            return false;
        outputCapacity = 0;
        outputChannels = 0;

        //set_defaults_();

        state = STREAM_DECODER_UNINITIALIZED;
        return true;
    }

    //boolean set_read_callback(InputStream is) {
    //    if (state != STREAM_DECODER_UNINITIALIZED)
    //        return false;
    //    this.is = is;
    //    return true;
    //}

    /*
        boolean set_write_callback(StreamDecoderWriteCallback value) {
            if (state != STREAM_DECODER_UNINITIALIZED)
                return false;
            write_callback = value;
            return true;
        }
    
        boolean set_metadata_callback(StreamDecoderMetadataCallback value) {
            if (state != STREAM_DECODER_UNINITIALIZED)
                return false;
            metadata_callback = value;
            return true;
        }
    
        boolean set_error_callback(StreamDecoderErrorCallback value) {
            if (state != STREAM_DECODER_UNINITIALIZED)
                return false;
            error_callback = value;
            return true;
        }
        */

    /*
        boolean set_client_data(Object value) {
            if (state != STREAM_DECODER_UNINITIALIZED)
                return false;
            client_data = value;
            return true;
        }
        */

    /*
    boolean set_metadata_respond(int type) {
        // double protection
        if ((int) type >= (1 << Format.STREAM_METADATA_TYPE_LEN))
            return false;
        if (state != STREAM_DECODER_UNINITIALIZED)
            return false;
        metadata_filter[type] = true;
        if (type == Format.METADATA_TYPE_APPLICATION)
            metadata_filter_ids.removeAllElements();
        return true;
    }
*/

    /*
    boolean set_metadata_respond_application(byte[] id) {
        if (state != STREAM_DECODER_UNINITIALIZED)
            return false;

        if (metadata_filter[Format.METADATA_TYPE_APPLICATION])
            return true;

        //if (metadata_filter_ids_count == metadata_filter_ids_capacity) {
        //    byte[] newIds = new byte[metadata_filter_ids_capacity * 2];
        //    System.arraycopy(metadata_filter_ids, 0, newIds, 0, metadata_filter_ids_capacity);
        //    metadata_filter_ids = newIds;
        //    metadata_filter_ids_capacity *= 2;
        //}

        //System.arraycopy(id, 0, metadata_filter_ids, metadata_filter_ids_count * (Format.STREAM_METADATA_APPLICATION_ID_LEN / 8), (Format.STREAM_METADATA_APPLICATION_ID_LEN / 8));
        //metadata_filter_ids_count++;
        metadata_filter_ids.add(id.clone());

        return true;
    }

    boolean set_metadata_respond_all() {
        if (state != STREAM_DECODER_UNINITIALIZED)
            return false;
        for (int i = 0; i < metadata_filter.length; i++)
            metadata_filter[i] = true;
        metadata_filter_ids.removeAllElements();
        return true;
    }

    boolean set_metadata_ignore(int type) {
        // double protection
        if ((int) type >= (1 << Format.STREAM_METADATA_TYPE_LEN))
            return false;
        if (state != STREAM_DECODER_UNINITIALIZED)
            return false;
        metadata_filter[type] = false;
        if (type == Format.METADATA_TYPE_APPLICATION)
            metadata_filter_ids.removeAllElements();
        return true;
    }

    boolean set_metadata_ignore_application(byte[] id) {
        if (state != STREAM_DECODER_UNINITIALIZED)
            return false;

        if (!metadata_filter[Format.METADATA_TYPE_APPLICATION])
            return true;
        //if (metadata_filter_ids_count == metadata_filter_ids_capacity) {
        //    byte[] newIds = new byte[metadata_filter_ids_capacity * 2];
        //    System.arraycopy(metadata_filter_ids, 0, newIds, 0, metadata_filter_ids_capacity);
        //    metadata_filter_ids = newIds;
        //    metadata_filter_ids_capacity *= 2;
        //}

        //System.arraycopy(id, 0, metadata_filter_ids, metadata_filter_ids_count * (Format.STREAM_METADATA_APPLICATION_ID_LEN / 8), (Format.STREAM_METADATA_APPLICATION_ID_LEN / 8));
        //metadata_filter_ids_count++;
        metadata_filter_ids.add(id.clone());

        return true;
    }

    boolean set_metadata_ignore_all() {
        if (state != STREAM_DECODER_UNINITIALIZED)
            return false;
        for (int i = 0; i < metadata_filter.length; i++)
            metadata_filter[i] = false;
        metadata_filter_ids.removeAllElements();
        return true;
    }
    */

    int getState() {
        return state;
    }

    int getChannels() {
        return channels;
    }

    int getChannelAssignment() {
        return channelAssignment;
    }

    int getBitsPerSample() {
        return bitsPerSample;
    }

    int getSampleRate() {
        return sampleRate;
    }

    int getBlockSize() {
        return blockSize;
    }
    
    StreamInfo getStreamInfo() {
        return streamInfo;
    }
    
    ChannelData[] getChannelData() {
        return channelData;
    }

/*
    boolean flush() {

        is.clear();
        state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;

        return true;
    }
    */

/*
    boolean reset() {
        if (!flush()) {
            state = STREAM_DECODER_MEMORY_ALLOCATION_ERROR;
            return false;
        }
        state = STREAM_DECODER_SEARCH_FOR_METADATA;

        samples_decoded = 0;

        return true;
    }
*/

    boolean processSingle() throws IOException {
 
        while (true) {
            switch (state) {
                case STREAM_DECODER_SEARCH_FOR_METADATA :
                    findMetadata();
                    break;
                case STREAM_DECODER_READ_METADATA :
                    readMetadata(); /* above function sets the status for us */
                    return true;
                case STREAM_DECODER_SEARCH_FOR_FRAME_SYNC :
                    frameSync(); /* above function sets the status for us */
                    break;
                case STREAM_DECODER_READ_FRAME :
                    readFrame();
                    return true; /* above function sets the status for us */
                    //break;
                case STREAM_DECODER_END_OF_STREAM :
                case STREAM_DECODER_ABORTED :
                    return true;
                default :
                    return false;
            }
        }
    }

    void processMetadata() throws IOException {

        while (true) {
            switch (state) {
                case STREAM_DECODER_SEARCH_FOR_METADATA :
                    findMetadata();
                    break;
                case STREAM_DECODER_READ_METADATA :
                    readMetadata(); // above function sets the status for us
                    break;
                case STREAM_DECODER_SEARCH_FOR_FRAME_SYNC :
                case STREAM_DECODER_READ_FRAME :
                case STREAM_DECODER_END_OF_STREAM :
                case STREAM_DECODER_ABORTED :
                default :
                    return;
             }
        }
    }

    boolean processUntilEndOfStream() throws IOException {
        boolean got_a_frame;

        while (true) {
            //System.out.println("Process for state: "+state+" "+StreamDecoderStateString[state]);
            switch (state) {
                case STREAM_DECODER_SEARCH_FOR_METADATA :
                    findMetadata();
                    break;
                case STREAM_DECODER_READ_METADATA :
                    readMetadata(); /* above function sets the status for us */
                    break;
                case STREAM_DECODER_SEARCH_FOR_FRAME_SYNC :
                    frameSync(); /* above function sets the status for us */
                    //System.exit(0);
                    break;
                case STREAM_DECODER_READ_FRAME :
                    readFrame();
                    break;
                case STREAM_DECODER_END_OF_STREAM :
                case STREAM_DECODER_ABORTED :
                    return true;
                default :
                    return false;
            }
        }
    }

    Frame getNextFrame() throws IOException {
        boolean got_a_frame;

        while (true) {
            //System.out.println("Process for state: "+state+" "+StreamDecoderStateString[state]);
            switch (state) {
                //case STREAM_DECODER_SEARCH_FOR_METADATA :
                //    findMetadata();
                //    break;
                //case STREAM_DECODER_READ_METADATA :
                //    readMetadata(); /* above function sets the status for us */
                //    break;
                case STREAM_DECODER_SEARCH_FOR_FRAME_SYNC :
                    frameSync(); /* above function sets the status for us */
                    //System.exit(0);
                    break;
                case STREAM_DECODER_READ_FRAME :
                    if (readFrame()) return frame;
                    break;
                case STREAM_DECODER_END_OF_STREAM :
                case STREAM_DECODER_ABORTED :
                    return null;
                default :
                    return null;
            }
        }
    }

    /***********************************************************************
     *
     * Protected class methods
     *
     ***********************************************************************/

    int get_input_bytes_unconsumed() {
        return is.getInputBytesUnconsumed();
    }

    /***********************************************************************
     *
     * Private class methods
     *
     ***********************************************************************/

    //void set_defaults_() {
    //    is = null;
    //write_callback = 0;
    //metadata_callback = 0;
    //error_callback = 0;
    //client_data = 0;

    //    for (int i = 0; i < metadata_filter.length; i++)
    //        metadata_filter[i] = false;
    //    metadata_filter[Format.METADATA_TYPE_STREAMINFO] = true;
    //    metadata_filter_ids.removeAllElements();
    //}

    boolean allocateOutput(int size, int channels) {
        if (size <= outputCapacity && channels <= outputChannels)
            return true;

        /* simply using realloc() is not practical because the number of channels may change mid-stream */

        for (int i = 0; i < MAX_CHANNELS; i++) {
            //output[i] = null;
            //residual[i] = null;
            channelData[i] = null;
        }

        for (int i = 0; i < channels; i++) {
            /* WATCHOUT:
             * lpc_restore_signal_asm_ia32_mmx() requires that the
             * output arrays have a buffer of up to 3 zeroes in front
             * (at negative indices) for alignment purposes; we use 4
             * to keep the data well-aligned.
             */
            //int[] tmp1 = new int[size];
            //output[i] = tmp1;

            //int[] tmp2 = new int[size];
            //residual[i] = tmp2;
            
            channelData[i] = new ChannelData(size);
        }

        outputCapacity = size;
        outputChannels = channels;

        return true;
    }

    /*
    boolean has_id_filtered_(byte[] id) {

        for (int i = 0; i < metadata_filter_ids.size(); i++) {
            byte[] checkId = (byte[]) metadata_filter_ids.get(i);
            if (checkId.equals(id))
                return true;
            //if (0
            //    == memcmp(
            //        metadata_filter_ids + i * (Format.STREAM_METADATA_APPLICATION_ID_LEN / 8),
            //        id,
            //        (Format.STREAM_METADATA_APPLICATION_ID_LEN / 8)))
            //    return true;
        }

        return false;
    }
    */

    void findMetadata() throws IOException {
        boolean first = true;

        int id;
        for (int i = id = 0; i < 4;) {
            int x;
            //if (cached) {
            //    x = (int) lookahead;
            //    cached = false;
            //} else {
                x = is.readRawUInt(8);
            //}
            if (x == Constants.STREAM_SYNC_STRING[i]) {
                first = true;
                i++;
                id = 0;
                continue;
            }
            if (x == ID3V2_TAG_[id]) {
                id++;
                i = 0;
                if (id == 3) skipID3v2Tag();
                continue;
            }
            if (x == 0xff) { // MAGIC NUMBER for the first 8 frame sync bits
                headerWarmup[0] = (byte) x;
                x = is.peekRawUInt(8);

                // we have to check if we just read two 0xff's in a row; the second may actually be the beginning of the sync code
                // else we have to check if the second byte is the end of a sync code
                if (x == 0xff) { // MAGIC NUMBER for the first 8 frame sync bits
                    //lookahead = (byte) x;
                    //cached = true;
                } else if ((x >> 2) == 0x3e) { // MAGIC NUMBER for the last 6 sync bits
                    headerWarmup[1] = (byte) is.readRawUInt(8);
                    state = STREAM_DECODER_READ_FRAME;
                }
            }
            i = 0;
            if (first) {
                System.err.println("STREAM_DECODER_ERROR_STATUS_LOST_SYNC");
                throw new IOException("STREAM_DECODER_ERROR_STATUS_LOST_SYNC");
            }
        }

        state = STREAM_DECODER_READ_METADATA;
    }

    void readMetadata() throws IOException {
        MetadataBase block = null;

        boolean isLast = (is.readRawUInt(Constants.STREAM_METADATA_IS_LAST_LEN) != 0);
        int type = is.readRawUInt(Constants.STREAM_METADATA_TYPE_LEN);
        int length = is.readRawUInt(Constants.STREAM_METADATA_LENGTH_LEN);

        //boolean skipIt = !metadata_filter[type];

        if (type == Constants.METADATA_TYPE_STREAMINFO) {
            block = streamInfo = new StreamInfo(is, isLast, length);
            //if (metadata_filter[Format.METADATA_TYPE_STREAMINFO])
            //metadata_callback(stream_info);
        } else if (type == Constants.METADATA_TYPE_SEEKTABLE) {
            block = seekTable = new SeekTable(is, isLast, length);
            //if (metadata_filter[Format.METADATA_TYPE_SEEKTABLE])
            //metadata_callback(seek_table);
        } else if (type == Constants.METADATA_TYPE_APPLICATION) {
            block = new Application(is, isLast, length);
            //block.is_last = is_last;
            //block.type = (MetadataType) type;
            //block.length = length;
            //is.read_byte_block_aligned_no_crc(
            //    block.id,
            //    Format.STREAM_METADATA_APPLICATION_ID_LEN / 8,
            //    read_callback);
            // decoder))
            // return false; /* the read_callback_ sets the state for us */

            //real_length -= Format.STREAM_METADATA_APPLICATION_ID_LEN / 8;

            //if (metadata_filter_ids.size() > 0 && has_id_filtered_(block.id))
            //  skip_it = !skip_it;
            //   }

            // if (skip_it) {
            //   is.read_byte_block_aligned_no_crc(null, real_length, read_callback);
            //    return false; /* the read_callback_ sets the state for us */
        } else if (type == Constants.METADATA_TYPE_PADDING) {
            System.out.println("METADATA_TYPE_PADDING: "+length);
            //byte[] pad = new byte[length];
            is.readByteBlockAlignedNoCRC(null, length);
            //for (int i = 0; i < 5; i++) System.out.println("Pad="+Integer.toHexString(pad[length-i-1]));
        //} else if (!metadata_filter[type]) {
        //    System.out.println("Skip "+type+": "+length);
        //    is.readByteBlockAlignedNoCRC(null, length);
        } else if (type == Constants.METADATA_TYPE_VORBIS_COMMENT) {
            block = new VorbisComment(is, isLast, length);
        } else if (type == Constants.METADATA_TYPE_CUESHEET) {
            block = new CueSheet(is, isLast, length);
        } else {
            block = new Unknown(is,  isLast, length);
            //  break;
        }
        if (block != null) metadata_callback(block);

        /* now we have to free any malloc'ed data in the block */
        /*
                        switch (type) {
                            case Format.METADATA_TYPE_PADDING :
                                break;
                            case Format.METADATA_TYPE_APPLICATION :
                                if (0 != block.data.application.data)
                                    free(block.data.application.data);
                                break;
                            case Format.METADATA_TYPE_VORBIS_COMMENT :
                                if (0 != block.data.vorbis_comment.vendor_string.entry)
                                    free(block.data.vorbis_comment.vendor_string.entry);
                                if (block.data.vorbis_comment.num_comments > 0)
                                    for (i = 0; i < block.data.vorbis_comment.num_comments; i++)
                                        if (0 != block.data.vorbis_comment.comments[i].entry)
                                            free(block.data.vorbis_comment.comments[i].entry);
                                if (0 != block.data.vorbis_comment.comments)
                                    free(block.data.vorbis_comment.comments);
                                break;
                            case Format.METADATA_TYPE_CUESHEET :
                                if (block.data.cue_sheet.num_tracks > 0)
                                    for (i = 0; i < block.data.cue_sheet.num_tracks; i++)
                                        if (0 != block.data.cue_sheet.tracks[i].indices)
                                            free(block.data.cue_sheet.tracks[i].indices);
                                if (0 != block.data.cue_sheet.tracks)
                                    free(block.data.cue_sheet.tracks);
                                break;
                            case Format.METADATA_TYPE_STREAMINFO :
                            case Format.METADATA_TYPE_SEEKTABLE :
                            default :
                                if (0 != block.data.unknown.data)
                                    free(block.data.unknown.data);
                                break;
                        }
                    }
                }
        */
        if (isLast) state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
    }


    private void skipID3v2Tag() throws IOException {

        // skip the version and flags bytes 
        is.readRawUInt(24);

        // get the size (in bytes) to skip
        int skip = 0;
        for (int i = 0; i < 4; i++) {
            int x = is.readRawUInt(8);
            skip <<= 7;
            skip |= (x & 0x7f);
        }
        
        // skip the rest of the tag
        is.readByteBlockAlignedNoCRC(null, skip);
    }

    void frameSync() throws IOException {
        boolean first = true;

        /* If we know the total number of samples in the stream, stop if we've read that many. */
        /* This will stop us, for example, from wasting time trying to sync on an ID3V1 tag. */
        if (streamInfo != null && (streamInfo.totalSamples != 0)) {
            if (samplesDecoded >= streamInfo.totalSamples) {
                //throw new IOException("STREAM_DECODER_END_OF_STREAM");
                state = STREAM_DECODER_END_OF_STREAM;
                return;
            }
        }

        /* make sure we're byte aligned */
        if (!is.isConsumedByteAligned()) {
            is.readRawUInt(is.bitsLeftForByteAlignment());
        }

        int x;
        while (true) {
            //if (cached) {
            //    x = (int) lookahead;
            //    cached = false;
            //} else {
                x = is.readRawUInt(8);
                //System.out.println("FindSync - read "+Integer.toHexString(x&0xff));
                // return false; /* the read_callback_ sets the state for us */
            //}
            if (x == 0xff) { /* MAGIC NUMBER for the first 8 frame sync bits */
                headerWarmup[0] = (byte) x;
                x = is.peekRawUInt(8);
                //       return false; /* the read_callback_ sets the state for us */

                /* we have to check if we just read two 0xff's in a row; the second may actually be the beginning of the sync code */
                /* else we have to check if the second byte is the end of a sync code */
                if (x == 0xff) { /* MAGIC NUMBER for the first 8 frame sync bits */
                    //lookahead = (byte) x;
                    //cached = true;
                } else if (x >> 2 == 0x3e) { /* MAGIC NUMBER for the last 6 sync bits */
                    headerWarmup[1] = (byte) is.readRawUInt(8);
                    state = STREAM_DECODER_READ_FRAME;
                    return;
                }
            }
            if (first) {
                System.out.println("FindSync LOST_SYNC: "+Integer.toHexString((x & 0xff)));
                //error_callback(STREAM_DECODER_ERROR_STATUS_LOST_SYNC);
                first = false;
            }
        }
    }

    boolean readFrame() throws IOException {
        boolean gotAFrame = false;
        int channel;
        int i;
        int mid, side, left, right;
        short frameCRC; /* the one we calculate from the input stream */
        int x;

        /* init the CRC */
        frameCRC = 0;
        frameCRC = CRC16.update(headerWarmup[0], frameCRC);
        frameCRC = CRC16.update(headerWarmup[1], frameCRC);
        is.resetReadCRC16(frameCRC);

        try {
            //if (!read_frame_header_())
            //    throw new IOException("Could not find frame header");
            frame.header = new Header(is, headerWarmup, streamInfo);
        } catch (BadHeaderException e) {
            System.out.println("Found bad header: "+e);
            state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
        }
        System.out.println("ReadFrame Header: "+frame.header.toString());
        if (state == STREAM_DECODER_SEARCH_FOR_FRAME_SYNC)
            return false;// gotAFrame;
        if (!allocateOutput(frame.header.blockSize, frame.header.channels))
            throw new IOException("Output allocation error");
        for (channel = 0; channel < frame.header.channels; channel++) {
            /*
             * first figure the correct bits-per-sample of the subframe
             */
            int bps = frame.header.bitsPerSample;
            switch (frame.header.channelAssignment) {
                case Header.CHANNEL_ASSIGNMENT_INDEPENDENT :
                    /* no adjustment needed */
                    break;
                case Header.CHANNEL_ASSIGNMENT_LEFT_SIDE :
                    if (channel == 1)
                        bps++;
                    break;
                case Header.CHANNEL_ASSIGNMENT_RIGHT_SIDE :
                    if (channel == 0)
                        bps++;
                    break;
                case Header.CHANNEL_ASSIGNMENT_MID_SIDE :
                    if (channel == 1)
                        bps++;
                    break;
                default :
                    }
            /*
             * now read it
             */
            try {
                readSubframe(channel, bps);
            } catch (IOException e) {
                System.out.println("ReadSubframe: "+e);
            }
            if (state != STREAM_DECODER_READ_FRAME) {
                state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
                return false;// gotAFrame;
            }
        }
        readZeroPadding();

        /*
         * Read the frame CRC-16 from the footer and check
         */
        frameCRC = is.getReadCRC16();
        x = is.readRawUInt(Constants.FRAME_FOOTER_CRC_LEN);
        //throw new DecoderException("Read CRC error"); /* the read_callback_ sets the state for us */
        if (frameCRC == (short) x) {
            /* Undo any special channel coding */
            switch (frame.header.channelAssignment) {
                case Header.CHANNEL_ASSIGNMENT_INDEPENDENT :
                    /* do nothing */
                    break;
                case Header.CHANNEL_ASSIGNMENT_LEFT_SIDE :
                    for (i = 0; i < frame.header.blockSize; i++)
                        channelData[1].output[i] = channelData[0].output[i] - channelData[1].output[i];
                    break;
                case Header.CHANNEL_ASSIGNMENT_RIGHT_SIDE :
                    for (i = 0; i < frame.header.blockSize; i++)
                        channelData[0].output[i] += channelData[1].output[i];
                    break;
                case Header.CHANNEL_ASSIGNMENT_MID_SIDE :
                    for (i = 0; i < frame.header.blockSize; i++) {
                        mid = channelData[0].output[i];
                        side = channelData[1].output[i];
                        mid <<= 1;
                        if ((side & 1) != 0) /* i.e. if 'side' is odd... */
                            mid++;
                        left = mid + side;
                        right = mid - side;
                        channelData[0].output[i] = left >> 1;
                        channelData[1].output[i] = right >> 1;
                    }
                    break;
                default :
                    break;
            }
        } else {
            /* Bad frame, emit error and zero the output signal */
            System.out.println("CRC Error: "+Integer.toHexString(frameCRC)+" vs " + Integer.toHexString(x));
            error_callback(STREAM_DECODER_ERROR_STATUS_FRAME_CRC_MISMATCH);
            for (channel = 0; channel < frame.header.channels; channel++) {
                for (int j = 0; j < frame.header.blockSize; j++)
                channelData[channel].output[j] = 0;
                // memset(output[channel], 0, sizeof(int32) * frame.header.blocksize);
            }
        }

        gotAFrame = true;

        /* put the latest values into the public section of the decoder instance */
        channels = frame.header.channels;
        channelAssignment = frame.header.channelAssignment;
        bitsPerSample = frame.header.bitsPerSample;
        sampleRate = frame.header.sampleRate;
        blockSize = frame.header.blockSize;

        samplesDecoded = frame.header.sampleNumber + frame.header.blockSize;

        /* write it */
        /** DRR FIX
        if (write_callback(frame, (int[][]) output) != STREAM_DECODER_WRITE_STATUS_CONTINUE)
            throw new IOException("decoder write error");
        */
        
        state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
        return true;// gotAFrame;
    }

    void readSubframe(int channel, int bps) throws IOException {
        int x;
        //boolean wastedBits;

        x = is.readRawUInt(8); /* MAGIC NUMBER */
        //return false; /* the read_callback_ sets the state for us */

        boolean haveWastedBits = ((x & 1) != 0);
        x &= 0xfe;

        int wastedBits = 0;
        if (haveWastedBits) {
            //int u;
            wastedBits = is.readUnaryUnsigned() + 1;
            // return false; /* the read_callback_ sets the state for us */
            //frame.subframes[channel].wastedBits = u + 1;
            bps -= frame.subframes[channel].wastedBits;
        } //else
            //frame.subframes[channel].wastedBits = 0;

        /*
         * Lots of magic numbers here
         */
        if ((x & 0x80) != 0) {
            System.out.println("ReadSubframe LOST_SYNC: "+Integer.toHexString(x&0xff));
            error_callback(STREAM_DECODER_ERROR_STATUS_LOST_SYNC);
            state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
            throw new IOException("ReadSubframe LOST_SYNC: "+Integer.toHexString(x&0xff));
            //return true;
        } else if (x == 0) {
            frame.subframes[channel] = new ChannelConstant(is, frame.header, channelData[channel], bps, wastedBits);
        } else if (x == 2) {
            frame.subframes[channel] = new ChannelVerbatim(is, frame.header, channelData[channel], bps, wastedBits);
        } else if (x < 16) {
            state = STREAM_DECODER_UNPARSEABLE_STREAM;
            throw new IOException("ReadSubframe Bad Subframe Type: "+Integer.toHexString(x&0xff));
        } else if (x <= 24) {
            //FLACSubframe_Fixed subframe = read_subframe_fixed_(channel, bps, (x >> 1) & 7);
            frame.subframes[channel] = new ChannelFixed(is, frame.header, channelData[channel], bps, wastedBits, (x >> 1) & 7);
        } else if (x < 64) {
            state = STREAM_DECODER_UNPARSEABLE_STREAM;
            throw new IOException("ReadSubframe Bad Subframe Type: "+Integer.toHexString(x&0xff));
        } else {
            //FLACSubframe_LPC subframe = read_subframe_lpc_(channel, bps, ((x >> 1) & 31) + 1);
            frame.subframes[channel] = new ChannelLPC(is, frame.header, channelData[channel], bps, wastedBits, ((x >> 1) & 31) + 1);
        }
        System.out.println("Subframe "+channel+": "+frame.subframes[channel].toString());
        //for (int i = 0; i < frame.header.blockSize; i++) System.out.println("\tRisidual["+i+"]="+channelData[channel].residual[i]);
        if (haveWastedBits) {
            int i;
            x = frame.subframes[channel].wastedBits;
            for (i = 0; i < frame.header.blockSize; i++)
                channelData[channel].output[i] <<= x;
        }
    }

    void readZeroPadding() throws IOException {
        if (!is.isConsumedByteAligned()) {
            int zero = is.readRawUInt(is.bitsLeftForByteAlignment());
            if (zero != 0) {
                System.out.println("ZeroPaddingError: "+Integer.toHexString(zero));
                error_callback(STREAM_DECODER_ERROR_STATUS_LOST_SYNC);
                state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
                //throw new DecoderException("STREAM_DECODER_ERROR_STATUS_LOST_SYNC");
            }
        }
    }

    /*
        boolean read_callback_(byte buffer[], unsigned * bytes, void * client_data) {
            StreamDecoder * decoder = (StreamDecoder *) client_data;
            StreamDecoderReadStatus status;
    
            status = read_callback(decoder, buffer, bytes, client_data);
            if (status == STREAM_DECODER_READ_STATUS_END_OF_STREAM)
                state = STREAM_DECODER_END_OF_STREAM;
            else if (status == STREAM_DECODER_READ_STATUS_ABORT)
                state = STREAM_DECODER_ABORTED;
            return status == STREAM_DECODER_READ_STATUS_CONTINUE;
        }
        */

    void error_callback(int e) {
        System.out.println("Error: "+e);
    }
    void metadata_callback(MetadataBase metadata) {
        System.out.println("Metadata: "+metadata.getClass().getName()+" "+metadata);
    }
    //int write_callback(FLACFrame frame, int[][] output) {
    //    return 0;
    //}
}
