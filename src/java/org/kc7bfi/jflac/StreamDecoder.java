package org.kc7bfi.jflac;

/**
 *  libFLAC - Free Lossless Audio Codec library
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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;

import org.kc7bfi.jflac.frame.BadHeaderException;
import org.kc7bfi.jflac.frame.ChannelConstant;
import org.kc7bfi.jflac.frame.ChannelFixed;
import org.kc7bfi.jflac.frame.ChannelLPC;
import org.kc7bfi.jflac.frame.ChannelVerbatim;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.frame.Header;
import org.kc7bfi.jflac.metadata.Application;
import org.kc7bfi.jflac.metadata.CueSheet;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.Padding;
import org.kc7bfi.jflac.metadata.SeekTable;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.metadata.Unknown;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.kc7bfi.jflac.util.ByteSpace;
import org.kc7bfi.jflac.util.CRC16;
import org.kc7bfi.jflac.util.InputBitStream;

public class StreamDecoder {
    private static final int METADATA_TYPE_STREAMINFO = 0;
    private static final int METADATA_TYPE_PADDING = 1;
    private static final int METADATA_TYPE_APPLICATION = 2;
    private static final int METADATA_TYPE_SEEKTABLE = 3;
    private static final int METADATA_TYPE_VORBIS_COMMENT = 4;
    private static final int METADATA_TYPE_CUESHEET = 5;
    private static final int METADATA_TYPE_UNDEFINED = 6;
    private static final byte[] STREAM_SYNC_STRING = new byte[] { (byte)'f', (byte)'L', (byte)'a', (byte)'C' };
    private static final int STREAM_METADATA_IS_LAST_LEN = 1; /* bits */
    private static final int STREAM_METADATA_TYPE_LEN = 7; /* bits */
    private static final int STREAM_METADATA_LENGTH_LEN = 24; /* bits */
    private static final int FRAME_FOOTER_CRC_LEN = 16; /* bits */
    
    private static final byte[] ID3V2_TAG_ = new byte[] { 'I', 'D', '3' };
    //private static final int MAX_CHANNELS = 8;
    
    private InputBitStream is;
    private ChannelData[] channelData = new ChannelData[Constants.MAX_CHANNELS];
    private int outputCapacity = 0;
    private int outputChannels = 0;
    private int lastFrameNumber;
    private long samplesDecoded;
    private StreamInfo streamInfo;
    private SeekTable seekTable;
    private Frame frame = new Frame();
    private byte[] headerWarmup = new byte[2]; /* contains the sync code and reserved bits */
    private int state;
    private int channels;
    private int channelAssignment;
    private int bitsPerSample;
    private int sampleRate; /* in Hz */
    private int blockSize; /* in samples (per channel) */
    
    private HashSet frameListeners = new HashSet();
    private HashSet pcmProcessors = new HashSet();
    
    // Decoder states
    private static final int STREAM_DECODER_SEARCH_FOR_METADATA = 0;
    private static final int STREAM_DECODER_READ_METADATA = 1;
    private static final int STREAM_DECODER_SEARCH_FOR_FRAME_SYNC = 2;
    private static final int STREAM_DECODER_READ_FRAME = 3;
    private static final int STREAM_DECODER_END_OF_STREAM = 4;
    private static final int STREAM_DECODER_ABORTED = 5;
    private static final int STREAM_DECODER_UNPARSEABLE_STREAM = 6;
    private static final int STREAM_DECODER_MEMORY_ALLOCATION_ERROR = 7;
    private static final int STREAM_DECODER_ALREADY_INITIALIZED = 8;
    private static final int STREAM_DECODER_INVALID_CALLBACK = 9;
    private static final int STREAM_DECODER_UNINITIALIZED = 10;
    
    /**
     * The constructor
     * @param is    The input stream to read data from
     */
    public StreamDecoder(InputStream is) {
        this.is = new InputBitStream(is);
        state = STREAM_DECODER_UNINITIALIZED;
        lastFrameNumber = 0;
        samplesDecoded = 0;
        state = STREAM_DECODER_SEARCH_FOR_METADATA;
    }
    
    public StreamInfo getStreamInfo() {
        return streamInfo;
    }
    
    public ChannelData[] getChannelData() {
        return channelData;
    }
    
    public InputBitStream getInputBitStream() {
        return is;
    }
    
    public void addFrameListener(FrameListener listener) {
        synchronized (frameListeners) {
            frameListeners.add(listener);
        }
    }
    
    public void removeFrameListener(FrameListener listener) {
        synchronized (frameListeners) {
            frameListeners.remove(listener);
        }
    }
    
    private void callMetadataListeners(Metadata metadata) {
        synchronized (frameListeners) {
            Iterator it = frameListeners.iterator();
            while (it.hasNext()) {
                FrameListener listener = (FrameListener)it.next();
                listener.processMetadata(metadata);
            }
        }
    }
    
    private void callFrameListeners(Frame frame) {
        synchronized (frameListeners) {
            Iterator it = frameListeners.iterator();
            while (it.hasNext()) {
                FrameListener listener = (FrameListener)it.next();
                listener.processFrame(frame);
            }
        }
    }
    
    public void addPCMProcessor(PCMProcessor processor) {
        synchronized (pcmProcessors) {
            pcmProcessors.add(processor);
        }
    }
    
    public void removePCMProcessor(PCMProcessor processor) {
        synchronized (pcmProcessors) {
            pcmProcessors.remove(processor);
        }
    }
    
    private void callStreamInfoProcessors(StreamInfo info) {
        synchronized (pcmProcessors) {
            Iterator it = pcmProcessors.iterator();
            while (it.hasNext()) {
                PCMProcessor processor = (PCMProcessor)it.next();
                processor.processStreamInfo(info);
            }
        }
    }
    
    private void callPCMProcessors(ByteSpace pcm) {
        synchronized (pcmProcessors) {
            Iterator it = pcmProcessors.iterator();
            while (it.hasNext()) {
                PCMProcessor processor = (PCMProcessor)it.next();
                processor.processPCM(pcm);
            }
        }
    }
    
    private void callPCMProcessors(Frame frame) {
        ByteSpace space = null;
        if (streamInfo.bitsPerSample == 8) {
            space = new ByteSpace(frame.header.blockSize * channels);
            for (int i = 0; i < frame.header.blockSize; i++) {
                for (int channel = 0; channel < channels; channel++) {
                    space.append((byte) (channelData[channel].getOutput()[i] + 0x80));
                }
            }
        } else if (streamInfo.bitsPerSample == 16) {
            space = new ByteSpace(frame.header.blockSize * channels * 2);
            for (int i = 0; i < frame.header.blockSize; i++) {
                for (int channel = 0; channel < channels; channel++) {
                    short val = (short) (channelData[channel].getOutput()[i]);
                    space.append((byte) (val & 0xff));
                    space.append((byte) ((val >> 8) & 0xff));
                }
            }
        } else if (streamInfo.bitsPerSample == 24) {
            space = new ByteSpace(frame.header.blockSize * channels * 3);
            for (int i = 0; i < frame.header.blockSize; i++) {
                for (int channel = 0; channel < channels; channel++) {
                    int val = (channelData[channel].getOutput()[i]);
                    space.append((byte) (val & 0xff));
                    space.append((byte) ((val >> 8) & 0xff));
                    space.append((byte) ((val >> 16) & 0xff));
                }
            }
        }
        callPCMProcessors(space);
    }
    
    /**
     * Read the FLAC stream info
     * @return  The FLAC Stream Info record
     */
    public StreamInfo readStreamInfo() {
        while (true) {
            try {
                findMetadata();
                Metadata metadata = readMetadata();
                if (metadata instanceof StreamInfo) return (StreamInfo) metadata;
            } catch (IOException e) {
                return null;
            }
        }
    }
    
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
    
    public void processMetadata() throws IOException {
        
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
    
    public void decode() throws IOException {
        while (true) {
            switch (state) {
            case STREAM_DECODER_SEARCH_FOR_METADATA :
                findMetadata();
                break;
            case STREAM_DECODER_READ_METADATA :
                Metadata metadata = readMetadata();
                if (metadata == null) break;
                callMetadataListeners(metadata);
                if (metadata instanceof StreamInfo) callStreamInfoProcessors((StreamInfo)metadata);
                break;
            case STREAM_DECODER_SEARCH_FOR_FRAME_SYNC :
                frameSync();
                break;
            case STREAM_DECODER_READ_FRAME :
                if (!readFrame()) break;
                callFrameListeners(frame);
                callPCMProcessors(frame);
                break;
            case STREAM_DECODER_END_OF_STREAM :
            case STREAM_DECODER_ABORTED :
                return;
            default :
                throw new IOException("Unknown state: " + state);
            }
        }
    }
    
    boolean processUntilEndOfStream() throws IOException {
        //boolean got_a_frame;
        
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
    
    public Frame getNextFrame() throws IOException {
        //boolean got_a_frame;
        
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
    
    /**
     * Bytes consumed.
     * @return  The number of bytes read
     */
    public long getBytesConsumed() {
        return is.getConsumedBlurbs();
    }
    
    /*
     public int getInputBytesUnconsumed() {
     return is.getInputBytesUnconsumed();
     }
     */
    
    private void allocateOutput(int size, int channels) {
        if (size <= outputCapacity && channels <= outputChannels) return;
        
        for (int i = 0; i < Constants.MAX_CHANNELS; i++) {
            channelData[i] = null;
        }
        
        for (int i = 0; i < channels; i++) {
            channelData[i] = new ChannelData(size);
        }
        
        outputCapacity = size;
        outputChannels = channels;
    }
    
    private void findMetadata() throws IOException {
        boolean first = true;
        
        int id;
        for (int i = id = 0; i < 4;) {
            int x = is.readRawUInt(8);
            if (x == STREAM_SYNC_STRING[i]) {
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
                if ((x >> 2) == 0x3e) { // MAGIC NUMBER for the last 6 sync bits
                    headerWarmup[1] = (byte) is.readRawUInt(8);
                    state = STREAM_DECODER_READ_FRAME;
                }
            }
            i = 0;
            //if (first) {
            //    System.err.println("STREAM_DECODER_ERROR_STATUS_LOST_SYNC");
            //    throw new IOException("STREAM_DECODER_ERROR_STATUS_LOST_SYNC");
            //}
        }
        
        state = STREAM_DECODER_READ_METADATA;
    }
    
    public Metadata readMetadata() throws IOException {
        Metadata metadata = null;
        
        boolean isLast = (is.readRawUInt(STREAM_METADATA_IS_LAST_LEN) != 0);
        int type = is.readRawUInt(STREAM_METADATA_TYPE_LEN);
        int length = is.readRawUInt(STREAM_METADATA_LENGTH_LEN);
        
        if (type == METADATA_TYPE_STREAMINFO) {
            metadata = streamInfo = new StreamInfo(is, length);
        } else if (type == METADATA_TYPE_SEEKTABLE) {
            metadata = seekTable = new SeekTable(is, length);
        } else if (type == METADATA_TYPE_APPLICATION) {
            metadata = new Application(is, length);
        } else if (type == METADATA_TYPE_PADDING) {
            metadata = new Padding(is, length);
        } else if (type == METADATA_TYPE_VORBIS_COMMENT) {
            metadata = new VorbisComment(is, length);
        } else if (type == METADATA_TYPE_CUESHEET) {
            metadata = new CueSheet(is, length);
        } else {
            metadata = new Unknown(is, length);
        }
        if (isLast) state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
        return metadata;
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
    
    private void frameSync() throws IOException {
        boolean first = true;int cnt=0;
        
        // If we know the total number of samples in the stream, stop if we've read that many.
        // This will stop us, for example, from wasting time trying to sync on an ID3V1 tag.
        if (streamInfo != null && (streamInfo.totalSamples != 0)) {
            if (samplesDecoded >= streamInfo.totalSamples) {
                state = STREAM_DECODER_END_OF_STREAM;
                return;
            }
        }
        
        // make sure we're byte aligned
        if (!is.isConsumedByteAligned()) {
            is.readRawUInt(is.bitsLeftForByteAlignment());
        }
        
        int x;
        while (true) {
            x = is.readRawUInt(8);
            if (x == 0xff) { /* MAGIC NUMBER for the first 8 frame sync bits */
                headerWarmup[0] = (byte) x;
                x = is.peekRawUInt(8);
                
                /* we have to check if we just read two 0xff's in a row; the second may actually be the beginning of the sync code */
                /* else we have to check if the second byte is the end of a sync code */
                if (x >> 2 == 0x3e) { /* MAGIC NUMBER for the last 6 sync bits */
                    headerWarmup[1] = (byte) is.readRawUInt(8);
                    state = STREAM_DECODER_READ_FRAME;
                    return;
                }
            }
            if (first) {
                System.out.println("FindSync LOST_SYNC: "+Integer.toHexString((x & 0xff)));
                if (cnt++ > 16) first = false;
            }
        }
    }
    
    public boolean readFrame() throws IOException {
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
            frame.header = new Header(is, headerWarmup, streamInfo);
        } catch (BadHeaderException e) {
            System.out.println("Found bad header: "+e);
            state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
        }
        if (state == STREAM_DECODER_SEARCH_FOR_FRAME_SYNC) return false;// gotAFrame;
        allocateOutput(frame.header.blockSize, frame.header.channels);
        for (channel = 0; channel < frame.header.channels; channel++) {
            // first figure the correct bits-per-sample of the subframe
            int bps = frame.header.bitsPerSample;
            switch (frame.header.channelAssignment) {
            case Constants.CHANNEL_ASSIGNMENT_INDEPENDENT :
                /* no adjustment needed */
                break;
            case Constants.CHANNEL_ASSIGNMENT_LEFT_SIDE :
                if (channel == 1)
                    bps++;
            break;
            case Constants.CHANNEL_ASSIGNMENT_RIGHT_SIDE :
                if (channel == 0)
                    bps++;
            break;
            case Constants.CHANNEL_ASSIGNMENT_MID_SIDE :
                if (channel == 1)
                    bps++;
            break;
            default :
            }
            // now read it
            try {
                //System.out.println("Read SubFrame");
                readSubframe(channel, bps);
            } catch (IOException e) {
                System.out.println("ReadSubframe: "+e);e.printStackTrace();
            }
            if (state != STREAM_DECODER_READ_FRAME) {
                state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
                return false;// gotAFrame;
            }
        }
        readZeroPadding();
        
        // Read the frame CRC-16 from the footer and check
        frameCRC = is.getReadCRC16();
        frame.crc = (short)is.readRawUInt(FRAME_FOOTER_CRC_LEN);
        if (frameCRC == frame.crc) {
            /* Undo any special channel coding */
            switch (frame.header.channelAssignment) {
            case Constants.CHANNEL_ASSIGNMENT_INDEPENDENT :
                /* do nothing */
                break;
            case Constants.CHANNEL_ASSIGNMENT_LEFT_SIDE :
                for (i = 0; i < frame.header.blockSize; i++)
                    channelData[1].getOutput()[i] = channelData[0].getOutput()[i] - channelData[1].getOutput()[i];
            break;
            case Constants.CHANNEL_ASSIGNMENT_RIGHT_SIDE :
                for (i = 0; i < frame.header.blockSize; i++)
                    channelData[0].getOutput()[i] += channelData[1].getOutput()[i];
            break;
            case Constants.CHANNEL_ASSIGNMENT_MID_SIDE :
                for (i = 0; i < frame.header.blockSize; i++) {
                    mid = channelData[0].getOutput()[i];
                    side = channelData[1].getOutput()[i];
                    //if (i < 20) System.out.print(Integer.toHexString(channelData[0].getOutput()[i])+" "+Integer.toHexString(channelData[1].getOutput()[i])+" ");
                    mid <<= 1;
                    if ((side & 1) != 0) // i.e. if 'side' is odd...
                        mid++;
                    left = mid + side;
                    right = mid - side;
                    channelData[0].getOutput()[i] = left >> 1;
                    channelData[1].getOutput()[i] = right >> 1;
                    //if (i < 20) System.out.println(Integer.toHexString(channelData[0].getOutput()[i])+" "+Integer.toHexString(channelData[1].getOutput()[i])+" ");
                }
            //System.exit(1);
            break;
            default :
                break;
            }
        } else {
            /* Bad frame, emit error and zero the output signal */
            System.out.println("CRC Error: "+Integer.toHexString(frameCRC)+" vs " + Integer.toHexString(frame.crc));
            for (channel = 0; channel < frame.header.channels; channel++) {
                for (int j = 0; j < frame.header.blockSize; j++)
                    channelData[channel].getOutput()[j] = 0;
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
        
        state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
        return true;// gotAFrame;
    }
    
    private void readSubframe(int channel, int bps) throws IOException {
        int x;
        
        x = is.readRawUInt(8); /* MAGIC NUMBER */
        //System.out.println("Magic Number="+Integer.toHexString(x&0xff));
        
        boolean haveWastedBits = ((x & 1) != 0);
        x &= 0xfe;
        
        int wastedBits = 0;
        if (haveWastedBits) {
            wastedBits = is.readUnaryUnsigned() + 1;
            bps -= wastedBits;
        }
        
        // Lots of magic numbers here
        if ((x & 0x80) != 0) {
            System.out.println("ReadSubframe LOST_SYNC: "+Integer.toHexString(x&0xff));
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
            frame.subframes[channel] = new ChannelLPC(is, frame.header, channelData[channel], bps, wastedBits, ((x >> 1) & 31) + 1);
        }
        if (haveWastedBits) {
            int i;
            x = frame.subframes[channel].WastedBits();
            for (i = 0; i < frame.header.blockSize; i++)
                channelData[channel].getOutput()[i] <<= x;
        }
    }
    
    private void readZeroPadding() throws IOException {
        if (!is.isConsumedByteAligned()) {
            int zero = is.readRawUInt(is.bitsLeftForByteAlignment());
            if (zero != 0) {
                System.out.println("ZeroPaddingError: "+Integer.toHexString(zero));
                state = STREAM_DECODER_SEARCH_FOR_FRAME_SYNC;
            }
        }
    }
}
