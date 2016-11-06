package org.jflac;

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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.jflac.frame.BadHeaderException;
import org.jflac.frame.ChannelConstant;
import org.jflac.frame.ChannelFixed;
import org.jflac.frame.ChannelLPC;
import org.jflac.frame.ChannelVerbatim;
import org.jflac.frame.Frame;
import org.jflac.frame.Header;
import org.jflac.io.BitInputStream;
import org.jflac.io.RandomFileInputStream;
import org.jflac.metadata.Application;
import org.jflac.metadata.CueSheet;
import org.jflac.metadata.Metadata;
import org.jflac.metadata.Padding;
import org.jflac.metadata.Picture;
import org.jflac.metadata.SeekPoint;
import org.jflac.metadata.SeekTable;
import org.jflac.metadata.StreamInfo;
import org.jflac.metadata.Unknown;
import org.jflac.metadata.VorbisComment;
import org.jflac.util.ByteData;
import org.jflac.util.CRC16;

/**
 * A Java FLAC decoder.
 * @author kc7bfi
 */
public class FLACDecoder {
    private static final int FRAME_FOOTER_CRC_LEN = 16; // bits
    private static final byte[] ID3V2_TAG = new byte[] { 'I', 'D', '3' };
    
    private BitInputStream bitStream;
    private ChannelData[] channelData = new ChannelData[Constants.MAX_CHANNELS];
    private int outputCapacity = 0;
    private int outputChannels = 0;
    private long samplesDecoded = 0;
    private StreamInfo streamInfo = null;
    private SeekTable seekTable = null;
    private Frame frame = new Frame();
    private byte[] headerWarmup = new byte[2]; // contains the sync code and reserved bits
    //private int state;
    private int channels;
    private InputStream inputStream = null;
    
    private int badFrames;
    private boolean eof = false;
    
    private FrameListeners frameListeners = new FrameListeners();
    private PCMProcessors pcmProcessors = new PCMProcessors();
    
    // Decoder states
    //private static final int DECODER_SEARCH_FOR_METADATA = 0;
    //private static final int DECODER_READ_METADATA = 1;
    //private static final int DECODER_SEARCH_FOR_FRAME_SYNC = 2;
    //private static final int DECODER_READ_FRAME = 3;
    //private static final int DECODER_END_OF_STREAM = 4;
    //private static final int DECODER_ABORTED = 5;
    //private static final int DECODER_UNPARSEABLE_STREAM = 6;
    //private static final int STREAM_DECODER_MEMORY_ALLOCATION_ERROR = 7;
    //private static final int STREAM_DECODER_ALREADY_INITIALIZED = 8;
    //private static final int STREAM_DECODER_INVALID_CALLBACK = 9;
    //private static final int STREAM_DECODER_UNINITIALIZED = 10;
    
    /**
     * The constructor.
     * @param inputStream    The input stream to read data from
     */
    public FLACDecoder(InputStream inputStream) {
        this.inputStream = inputStream;
        this.bitStream = new BitInputStream(inputStream);
        //state = DECODER_SEARCH_FOR_METADATA;     
        samplesDecoded = 0;
        //state = DECODER_SEARCH_FOR_METADATA;
    }
    
    /**
     * Return the parsed StreamInfo Metadata record.
     * @return  The StreamInfo
     */
    public StreamInfo getStreamInfo() {
        return streamInfo;
    }
    
    /**
     * Return the ChannelData object.
     * @return  The ChannelData object
     */
    public ChannelData[] getChannelData() {
        return channelData;
    }
    
    /**
     * Return the input but stream.
     * @return  The bit stream
     */
    public BitInputStream getBitInputStream() {
        return bitStream;
    }
    
    /**
     * Add a frame listener.
     * @param listener  The frame listener to add
     */
    public void addFrameListener(FrameListener listener) {
        frameListeners.addFrameListener(listener);
    }
    
    /**
     * Remove a frame listener.
     * @param listener  The frame listener to remove
     */
    public void removeFrameListener(FrameListener listener) {
        frameListeners.removeFrameListener(listener);
    }
    
    /**
     * Add a PCM processor.
     * @param processor  The processor listener to add
     */
    public void addPCMProcessor(PCMProcessor processor) {
        pcmProcessors.addPCMProcessor(processor);
    }
    
    /**
     * Remove a PCM processor.
     * @param processor  The processor listener to remove
     */
    public void removePCMProcessor(PCMProcessor processor) {
        pcmProcessors.removePCMProcessor(processor);
    }
    
    private void callPCMProcessors(Frame frame) {
    	ByteData bd = decodeFrame(frame, null);
        pcmProcessors.processPCM(bd);
    }
    
    /**
     * Fill the given ByteData object with PCM data from the frame.
     *
     * @param frame the frame to send to the PCM processors
     * @param pcmData the byte data to be filled, or null if it should be allocated
     * @return the ByteData that was filled (may be a new instance from <code>space</code>) 
     */
    public ByteData decodeFrame(Frame frame, ByteData pcmData) {
    	// required size of the byte buffer
    	int byteSize = frame.header.blockSize * channels * ((streamInfo.getBitsPerSample() + 7) / 2);
    	if (pcmData == null || pcmData.getData().length < byteSize ) {
    		pcmData = new ByteData(byteSize);
    	} else {
    		pcmData.setLen(0);
    	}
        if (streamInfo.getBitsPerSample() == 8) {
            for (int i = 0; i < frame.header.blockSize; i++) {
                for (int channel = 0; channel < channels; channel++) {
                    pcmData.append((byte) (channelData[channel].getOutput()[i] + 0x80));
                }
            }
        } else if (streamInfo.getBitsPerSample() == 16) {
            for (int i = 0; i < frame.header.blockSize; i++) {
                for (int channel = 0; channel < channels; channel++) {
                    short val = (short) (channelData[channel].getOutput()[i]);
                    pcmData.append((byte) (val & 0xff));
                    pcmData.append((byte) ((val >> 8) & 0xff));
                }
            }
        } else if (streamInfo.getBitsPerSample() == 24) {
            for (int i = 0; i < frame.header.blockSize; i++) {
                for (int channel = 0; channel < channels; channel++) {
                    int val = (channelData[channel].getOutput()[i]);
                    pcmData.append((byte) (val & 0xff));
                    pcmData.append((byte) ((val >> 8) & 0xff));
                    pcmData.append((byte) ((val >> 16) & 0xff));
                }
            }
        }
        return pcmData;
    }
    
    /**
     * Read the FLAC stream info.
     * @return  The FLAC Stream Info record
     * @throws IOException On read error
     */
    public StreamInfo readStreamInfo() throws IOException {
        readStreamSync();
        Metadata metadata = readNextMetadata();
        if (!(metadata instanceof StreamInfo)) throw new IOException("StreamInfo metadata block missing");
        return (StreamInfo) metadata;
    }
    
    /**
     * Read an array of metadata blocks.
     * @return  The array of metadata blocks
     * @throws IOException  On read error
     */
    public Metadata[] readMetadata() throws IOException {
        readStreamSync();
        Vector metadataList = new Vector();
        Metadata metadata;
        do {
            metadata = readNextMetadata();
            metadataList.add(metadata);
        } while (!metadata.isLast());
        return (Metadata[])metadataList.toArray(new Metadata[0]);
    }
    
    /**
     * Read an array of metadata blocks.
     * @param streamInfo    The StreamInfo metadata block previously read
     * @return  The array of metadata blocks
     * @throws IOException  On read error
     */
    public Metadata[] readMetadata(StreamInfo streamInfo) throws IOException {
        if (streamInfo.isLast()) return new Metadata[0];
        Vector metadataList = new Vector();
        Metadata metadata;
        do {
            metadata = readNextMetadata();
            metadataList.add(metadata);
        } while (!metadata.isLast());
        return (Metadata[])metadataList.toArray(new Metadata[0]);
    }
    
    /**
     * process a single metadata/frame.
     * @return True of one processed
     * @throws IOException  on read error
     */
    /*
     public boolean processSingle() throws IOException {
     
     while (true) {
     switch (state) {
     case DECODER_SEARCH_FOR_METADATA :
     readStreamSync();
     break;
     case DECODER_READ_METADATA :
     readNextMetadata(); // above function sets the status for us
     return true;
     case DECODER_SEARCH_FOR_FRAME_SYNC :
     frameSync(); // above function sets the status for us
     break;
     case DECODER_READ_FRAME :
     readFrame();
     return true; // above function sets the status for us
     //break;
      case DECODER_END_OF_STREAM :
      case DECODER_ABORTED :
      return true;
      default :
      return false;
      }
      }
      }
      */
    
    /**
     * Process all the metadata records.
     * @throws IOException On read error
     */
    /*
     public void processMetadata() throws IOException {
     
     while (true) {
     switch (state) {
     case DECODER_SEARCH_FOR_METADATA :
     readStreamSync();
     break;
     case DECODER_READ_METADATA :
     readNextMetadata(); // above function sets the status for us
     break;
     case DECODER_SEARCH_FOR_FRAME_SYNC :
     case DECODER_READ_FRAME :
     case DECODER_END_OF_STREAM :
     case DECODER_ABORTED :
     default :
     return;
     }
     }
     }
     */
    
    /**
     * Decode the FLAC file.
     * @throws IOException  On read error
     */
    public void decode() throws IOException {
        readMetadata();
        try {
            while (true) {
                //switch (state) {
                //case DECODER_SEARCH_FOR_METADATA :
                //    readStreamSync();
                //    break;
                //case DECODER_READ_METADATA :
                //    Metadata metadata = readNextMetadata();
                //    if (metadata == null) break;
                //    break;
                //case DECODER_SEARCH_FOR_FRAME_SYNC :
                findFrameSync();
                //    break;
                //case DECODER_READ_FRAME :
                try {
                    readFrame();
                    frameListeners.processFrame(frame);
                    callPCMProcessors(frame);
                } catch (FrameDecodeException e) {
                    badFrames++;
                }
                //    break;
                //case DECODER_END_OF_STREAM :
                //case DECODER_ABORTED :
                //    return;
                //default :
                //    throw new IOException("Unknown state: " + state);
                //}
            }
        } catch (EOFException e) {
            eof = true;
        }
    }
    
    /**
     * Decode the data frames.
     * @throws IOException  On read error
     */
    public void decodeFrames() throws IOException {
        //state = DECODER_SEARCH_FOR_FRAME_SYNC;
        try {
            while (true) {
                //switch (state) {
                //case DECODER_SEARCH_FOR_METADATA :
                //    readStreamSync();
                //    break;
                //case DECODER_READ_METADATA :
                //    Metadata metadata = readNextMetadata();
                //    if (metadata == null) break;
                //    break;
                //case DECODER_SEARCH_FOR_FRAME_SYNC :
                findFrameSync();
                //    break;
                //case DECODER_READ_FRAME :
                try {
                    readFrame();
                    frameListeners.processFrame(frame);
                    callPCMProcessors(frame);
                } catch (FrameDecodeException e) {
                    badFrames++;
                }
                //    break;
                //case DECODER_END_OF_STREAM :
                //case DECODER_ABORTED :
                //    return;
                //default :
                //    throw new IOException("Unknown state: " + state);
                //}
            }
        } catch (EOFException e) {
            eof = true;
        }
    }
    
    /**
     * Decode the data frames between two seek points.
     * @param from  The starting seek point
     * @param to    The ending seek point (non-inclusive)
     * @throws IOException  On read error
     */
    public void decode(SeekPoint from, SeekPoint to) throws IOException {
        // position random access file
        if (!(inputStream instanceof RandomFileInputStream)) throw new IOException("Not a RandomFileInputStream: " + inputStream.getClass().getName());
        ((RandomFileInputStream)inputStream).seek(from.getStreamOffset());
        bitStream.reset();
        samplesDecoded = from.getSampleNumber();
        
        //state = DECODER_SEARCH_FOR_FRAME_SYNC;
        try {
            while (true) {
                //switch (state) {
                //case DECODER_SEARCH_FOR_METADATA :
                //    readStreamSync();
                //    break;
                //case DECODER_READ_METADATA :
                //    Metadata metadata = readNextMetadata();
                //    if (metadata == null) break;
                //    break;
                //case DECODER_SEARCH_FOR_FRAME_SYNC :
                findFrameSync();
                //    break;
                //case DECODER_READ_FRAME :
                try {
                    readFrame();
                    frameListeners.processFrame(frame);
                    callPCMProcessors(frame);
                } catch (FrameDecodeException e) {
                    badFrames++;
                }
                //frameListeners.processFrame(frame);
                //callPCMProcessors(frame);
                //System.out.println(samplesDecoded +" "+ to.getSampleNumber());
                if (to != null && samplesDecoded >= to.getSampleNumber()) return;
                //    break;
                //case DECODER_END_OF_STREAM :
                //case DECODER_ABORTED :
                //    return;
                //default :
                //    throw new IOException("Unknown state: " + state);
                //}
            }
        } catch (EOFException e) {
            eof = true;
        }
    }
    
    /**
     * Attempt to seek to the requested (absolute) sample position in the file.
     * Actually seeks to the frame which contains the requested sample position.
     * @param samplesAbsolute	The number of samples from start of file to seek past
     * @return 	The new sample position in the file (from which the next frame will be read)
     * @throws IOException 
     */
    public long seek(long samplesAbsolute) throws IOException
    {
        // move back to start of file
        if (!(inputStream instanceof RandomFileInputStream))
        	throw new IOException("Not a RandomFileInputStream: " + inputStream.getClass().getName());
        ((RandomFileInputStream)inputStream).seek(0);
        bitStream.reset();
        samplesDecoded = 0;
        
        // track seek delta (difference between requested and current position in samples)
        long seekDelta = samplesAbsolute - samplesDecoded;
        
        // (re-)read metadata (including stream info and seek table)     	
    	readMetadata();
    	// check for stream info
        if (streamInfo == null)
        	throw new IOException("Could not obtain stream info required for seeking");
    
    	// validate requested (absolute) sample position
    	if (samplesAbsolute < 0 || samplesAbsolute >= streamInfo.getTotalSamples())
    		throw new IllegalArgumentException("Invalid sample position for seek");
    	
    	// check if seek to start of file (already done above)
    	if (seekDelta == 0)
    		return samplesDecoded; // samplesDecoded == 0

    	// use max frame size for seeking backwards
    	int estimatedFrameSize = streamInfo.getMaxFrameSize();    	
    	
    	// use data start and end positions as initial lower and upper bounds for seeking 	
    	// Note: should be at first data frame immediately after readMetadata()
    	findFrameSync(); // this reads up to the 2 sync bytes of the first data frame into bitStream
    	// keep track of current frame starting position
    	long bytePositionCurrentFrame = bitStream.getTotalBytesRead() - 2;	
    	// Note: uses virtual temporary SeekPoint variables to store sample/byte offset pairs
    	long samplePositionUpper = streamInfo.getTotalSamples();
    	long bytePositionUpper = ((RandomFileInputStream)inputStream).getLength() - estimatedFrameSize
    							 - bytePositionCurrentFrame;
    	// Note: maximum frame size used to increase chance of being before final frame
    	//	 	 (though still may not be if file contains ID3 tags after audio data)
    	SeekPoint beforeSeekPosition = new SeekPoint(0, 0, estimatedFrameSize);
    	SeekPoint afterSeekPosition = new SeekPoint(samplePositionUpper, bytePositionUpper,
    												streamInfo.getMinBlockSize());
    	
    	// use seekTable to (greatly) improve these initial lower and upper bounds
    	if (!(seekTable == null))
    	{    		
    		// use actual SeekPoint data from seekTable
    		for (int i=0; i<seekTable.numberOfPoints(); i++)
    		{
    			SeekPoint currentSeekPoint = seekTable.getSeekPoint(i);
    			// check against requested sample position and current lower and upper bounds
    			if (currentSeekPoint.getSampleNumber() < samplesAbsolute)
    			{
    				// SeekPoint is before requested sample position:    				
    				// check if closer than current lower bound
    				if (currentSeekPoint.getSampleNumber() > beforeSeekPosition.getSampleNumber())
    				{
    					beforeSeekPosition = currentSeekPoint; // set as new lower bound
    					
        				// check if requested sample position is within this SeekPoint
        				if ( samplesAbsolute < beforeSeekPosition.getSampleNumber() 
        										+ beforeSeekPosition.getFrameSamples() )
        				{
        					afterSeekPosition = currentSeekPoint; // set as new upper bound as well
        					break; // no need to check any further SeekPoints
        				}
    				}    					
    			}
    			else if (currentSeekPoint.getSampleNumber() > samplesAbsolute)
    			{
    				// SeekPoint is after requested sample position:
    				// check if closer than current upper bound
    				if (currentSeekPoint.getSampleNumber() < afterSeekPosition.getSampleNumber())
    					afterSeekPosition = currentSeekPoint; // set as new upper bound
    			}
    			else if (currentSeekPoint.getSampleNumber() == samplesAbsolute)
    			{
    				// SeekPoint is exactly the requested sample position:
    				// set lower and upper bound to this SeekPoint
    				beforeSeekPosition = currentSeekPoint;
    				afterSeekPosition = currentSeekPoint;    				
    				break; // no need to check any further SeekPoints
    			}
    			else
    				continue; // ignore SeekPoint (should never get here)
    		}    			
    	}    
    	
    	// use lower and upper bounds to estimate position of frame containing sample position:
    	// set initial estimate as lower bound
    	long bytePositionEstimate = bytePositionCurrentFrame + beforeSeekPosition.getStreamOffset();    	
    	// check if sample position not found within lower bound frame
    	// Note: if it had been found, then upper (after) would equal lower (before) sample position
    	if (afterSeekPosition.getSampleNumber() > beforeSeekPosition.getSampleNumber() )
    	{
        	// calculate estimated position within lower and upper bounds:
        	double percentBetweenSeekPositions = (samplesAbsolute - beforeSeekPosition.getSampleNumber())
        										 / (double) (afterSeekPosition.getSampleNumber() 
        												 	 - beforeSeekPosition.getSampleNumber());
        	// remove the frame size to seek to before start of frame containing sample position 
        	long bytePositionBetweenSeekPositions = (long) ((afterSeekPosition.getStreamOffset()
        							         				- beforeSeekPosition.getStreamOffset())
        										  			* percentBetweenSeekPositions)
        											- estimatedFrameSize;
        	// add estimated position within lower and upper bounds if not negative
        	// Note: could be negative due to subtraction of estimated frame size
        	if (bytePositionBetweenSeekPositions > 0 )
        		bytePositionEstimate += bytePositionBetweenSeekPositions;
    	}

    	// seek to estimate for frame containing requested sample position
    	((RandomFileInputStream)inputStream).seek(bytePositionEstimate);
    	bitStream.reset();
    	
    	/* loop until at requested sample position:
    	 * 0. starts at bytePositionEstimate from above initial logic;
    	 * 1. find and read the next frame from the current location;
    	 * 2. get frame sample range:
    	 * 	  (a) if requested sample position > current frame sample range, goto 1.;
    	 * 	  (b) if requested sample position < current frame sample range, seek backwards and goto 1.;
    	 * 	      (to prevent infinite loops, seeks further backwards on each time 2.(b) is reached)
    	 *    (c) if requested sample position within current frame sample range, exit loop.
    	 */ 
    	// Note: exits loop when current frame contains requested sample position
    	int framesToSeekBack = 1; // number of frames to go back if past requested position
    	Map<String, Object> seekBackwardsResults = new HashMap<String, Object>();
    	while (!(seekDelta == 0)) // Note: this condition is not currently used
    	{
        	// get the next data frame from the current position    		
    		findFrameSync(); // positions 2 bytes into the frame (as reads the 2 sync bytes)
    		// update position of current frame
    		bytePositionCurrentFrame = bytePositionEstimate + bitStream.getTotalBytesRead() - 2;
    		try { // attempt to read the current frame
				readFrame();
			} catch (FrameDecodeException e) {
				badFrames++;
				continue; // try to read next frame
			}
    		// update current position to end of frame
    		bytePositionEstimate += bitStream.getTotalBytesRead();
    		
    		if (frame == null)
    		{
    			// no frame read from file (likely no data frames remaining)
    			if (eof)
    			{
    				// end of file: attempt to seek back to previous frame (or further if required)
    				seekBackwardsResults = seekBackwards(bytePositionCurrentFrame, framesToSeekBack,
    													 estimatedFrameSize);
    				bytePositionEstimate = ((Long) seekBackwardsResults.get("bytePositionEstimate")).longValue();
    				framesToSeekBack = ((Integer) seekBackwardsResults.get("framesToSeekBack")).intValue();
    				// iterate again to get a previous frame
    				continue;
    			}
    			else // not end of file (unexpected file error)
    				throw new IOException("Fatal seek data frame reading error"); 
    		}
    		else
    		{
    			// frame read from file:
    			// use header to update current position
    			samplesDecoded = frame.header.sampleNumber;
    			
    			// update seek delta to determine if further iterations required
    			seekDelta = samplesAbsolute - samplesDecoded;
    			if (seekDelta < 0)
    			{
    				// requested sample is behind current frame:
    				// seek backwards (firstly by one frame, then further if required)
    				seekBackwardsResults = seekBackwards(bytePositionCurrentFrame, framesToSeekBack,
    													 estimatedFrameSize);
    				bytePositionEstimate = ((Long) seekBackwardsResults.get("bytePositionEstimate")).longValue();
    				framesToSeekBack = ((Integer) seekBackwardsResults.get("framesToSeekBack")).intValue();
    				// iterate again to get a previous frame
    				continue;
    			}
    			else if (seekDelta < frame.header.blockSize) // && seekDelta >= 0
    			{
    				// requested sample is in current frame (success):
    				// reposition pointer to before the frame
    				((RandomFileInputStream)inputStream).seek(bytePositionCurrentFrame);
    				bitStream.reset();
    				// success: exit loop at current position (immediately before frame)
    				break;
    			}
    			else if (seekDelta >= frame.header.blockSize)
    			{
    				// requested sample is ahead of current frame:
    				framesToSeekBack = 0; // should not need to go backwards now
    				((RandomFileInputStream)inputStream).seek(bytePositionEstimate);
    				bitStream.reset();
    				// iterate again to get the next data frame
    				continue;
    			}
    			else // this should not be possible, as above should cover all cases
    				throw new IOException("Fatal seek logic error");    			
    		}
    	}
    	
    	// return current sample position in file
    	return samplesDecoded;
    }
    
    /** Used by seek() to seek backwards, without causing an infinite loop.
     * @param bytePositionEstimate	The next position to seek to (in bytes)
     * @param framesToSeekBack		The number of frames to seek backwards
     * @param estimatedFrameSize	The size (in bytes) of a frame
     * @return 	Map containing new bytePositionEstimate and framesToSeekBack
     * @throws IOException
     */
    private Map<String, Object> seekBackwards(long bytePositionEstimate, int framesToSeekBack,
    										  long estimatedFrameSize) 
    		throws IOException 
    {
    	if (framesToSeekBack > 0)
		{
			// attempt to seek back to previous frame
			bytePositionEstimate -= framesToSeekBack * estimatedFrameSize;
			if (bytePositionEstimate < 0)
			{
				// prevent seeking before start of file
				bytePositionEstimate = 0;
				// should not need to seek backwards again
				framesToSeekBack = 0;
			}
			else
			{
				// seek back by further next time if position past required sample again
				framesToSeekBack++;
			}        
		}
		else // throw error: was behind (hence framesToSeekBack < 1) and now ahead
			throw new IOException("Fatal seek error: sample position not found");
    	
    	// seek back to new estimated position (should be before previous frame)
    	((RandomFileInputStream)inputStream).seek(bytePositionEstimate);
    	bitStream.reset();
    	
    	// return new estimated position and frames to seek back
    	Map<String, Object> returnVariables = new HashMap<String, Object>();
    	returnVariables.put("bytePositionEstimate", bytePositionEstimate);
    	returnVariables.put("framesToSeekBack", framesToSeekBack);
    	return returnVariables;
	}
    
    /*
     private boolean processUntilEndOfStream() throws IOException {
     //boolean got_a_frame;
      
      while (true) {
      switch (state) {
      case DECODER_SEARCH_FOR_METADATA :
      readStreamSync();
      break;
      case DECODER_READ_METADATA :
      readNextMetadata(); // above function sets the status for us
      break;
      case DECODER_SEARCH_FOR_FRAME_SYNC :
      frameSync(); // above function sets the status for us
      //System.exit(0);
       break;
       case DECODER_READ_FRAME :
       readFrame();
       break;
       case DECODER_END_OF_STREAM :
       case DECODER_ABORTED :
       return true;
       default :
       return false;
       }
       }
       }
       */

	/**
     * Read the next data frame.
     * @return  The next frame
     * @throws IOException  on read error
     */
    public Frame readNextFrame() throws IOException {
        //boolean got_a_frame;
        
        try {
            while (true) {
                //switch (state) {
                //case STREAM_DECODER_SEARCH_FOR_METADATA :
                //    findMetadata();
                //    break;
                //case STREAM_DECODER_READ_METADATA :
                //    readMetadata(); /* above function sets the status for us */
                //    break;
                //case DECODER_SEARCH_FOR_FRAME_SYNC :
                findFrameSync(); /* above function sets the status for us */
                //System.exit(0);
                //break;
                //case DECODER_READ_FRAME :
                try {
                    readFrame();
                    return frame;
                } catch (FrameDecodeException e) {
                    badFrames++;
                }
                //break;
                //case DECODER_END_OF_STREAM :
                //case DECODER_ABORTED :
                //    return null;
                //default :
                //    return null;
                //}
            }
        } catch (EOFException e) {
            eof = true;
        }
        return null;
    }
    
    /**
     * Bytes consumed.
     * @return  The number of bytes read
     */
    //public long getBytesConsumed() {
    //    return is.getConsumedBlurbs();
    //}
    
    /**
     * Bytes read.
     * @return  The number of bytes read
     */
    public long getTotalBytesRead() {
        return bitStream.getTotalBytesRead();
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
    
    /**
     * Read the stream sync string.
     * @throws IOException  On read error
     */
    private void readStreamSync() throws IOException {
        int id = 0;
        for (int i = 0; i < 4;) {
            int x = bitStream.readRawUInt(8);
            if (x == Constants.STREAM_SYNC_STRING[i]) {
                i++;
                id = 0;
            } else if (x == ID3V2_TAG[id]) {
                id++;
                i = 0;
                if (id == 3) {
                    skipID3v2Tag();
                    id = 0;
                }
            } else {
                throw new IOException("Could not find Stream Sync");
                //i = 0;
                //id = 0;
            }
        }
    }
    
    /**
     * Read a single metadata record.
     * @return  The next metadata record
     * @throws IOException  on read error
     */
    public Metadata readNextMetadata() throws IOException {
        Metadata metadata = null;
        
        boolean isLast = (bitStream.readRawUInt(Metadata.STREAM_METADATA_IS_LAST_LEN) != 0);
        int type = bitStream.readRawUInt(Metadata.STREAM_METADATA_TYPE_LEN);
        int length = bitStream.readRawUInt(Metadata.STREAM_METADATA_LENGTH_LEN);
        
        if (type == Metadata.METADATA_TYPE_STREAMINFO) {
            streamInfo = new StreamInfo(bitStream, length, isLast);
            metadata = streamInfo;
            pcmProcessors.processStreamInfo((StreamInfo)metadata);
        } else if (type == Metadata.METADATA_TYPE_SEEKTABLE) {
        	seekTable = new SeekTable(bitStream, length, isLast);
            metadata = seekTable;
        } else if (type == Metadata.METADATA_TYPE_APPLICATION) {
            metadata = new Application(bitStream, length, isLast);
        } else if (type == Metadata.METADATA_TYPE_PADDING) {
            metadata = new Padding(bitStream, length, isLast);
        } else if (type == Metadata.METADATA_TYPE_VORBIS_COMMENT) {
            metadata = new VorbisComment(bitStream, length, isLast);
        } else if (type == Metadata.METADATA_TYPE_CUESHEET) {
	    metadata = new CueSheet(bitStream, length, isLast);
	} else if (type == Metadata.METADATA_TYPE_PICTURE) {
	    metadata = new Picture(bitStream, length, isLast);
        } else {
            metadata = new Unknown(bitStream, length, isLast);
        }
        frameListeners.processMetadata(metadata);
        //if (isLast) state = DECODER_SEARCH_FOR_FRAME_SYNC;
        return metadata;
    }
    
    
    private void skipID3v2Tag() throws IOException {
        
        // skip the version and flags bytes 
        int verMajor = bitStream.readRawInt(8);
        int verMinor = bitStream.readRawInt(8);
        int flags = bitStream.readRawInt(8);
        
        // get the size (in bytes) to skip
        int skip = 0;
        for (int i = 0; i < 4; i++) {
            int x = bitStream.readRawUInt(8);
            skip <<= 7;
            skip |= (x & 0x7f);
        }
        
        // skip the rest of the tag
        bitStream.readByteBlockAlignedNoCRC(null, skip);
    }
    
    private void findFrameSync() throws IOException {
        boolean first = true;
        //int cnt=0;
        
        // If we know the total number of samples in the stream, stop if we've read that many.
        // This will stop us, for example, from wasting time trying to sync on an ID3V1 tag.
        if (streamInfo != null && (streamInfo.getTotalSamples() > 0)) {
            if (samplesDecoded >= streamInfo.getTotalSamples()) {
                //state = DECODER_END_OF_STREAM;
                return;
            }
        }
        
        // make sure we're byte aligned
        if (!bitStream.isConsumedByteAligned()) {
            bitStream.readRawUInt(bitStream.bitsLeftForByteAlignment());
        }
        
        int x;
        try {
            while (true) {
                x = bitStream.readRawUInt(8);
                if (x == 0xff) { // MAGIC NUMBER for the first 8 frame sync bits
                    headerWarmup[0] = (byte) x;
                    x = bitStream.peekRawUInt(8);
                    
                    /* we have to check if we just read two 0xff's in a row; the second may actually be the beginning of the sync code */
                    /* else we have to check if the second byte is the end of a sync code */
                    if (x >> 2 == 0x3e) { /* MAGIC NUMBER for the last 6 sync bits */
                        headerWarmup[1] = (byte) bitStream.readRawUInt(8);
                        //state = DECODER_READ_FRAME;
                        return;
                    }
                }
                if (first) {
                    frameListeners.processError("FindSync LOST_SYNC: " + Integer.toHexString((x & 0xff)));
                    first = false;
                }
            }
        } catch (EOFException e) {
            if (!first) frameListeners.processError("FindSync LOST_SYNC: Left over data in file");
            //state = DECODER_END_OF_STREAM;
        }
    }
    
    /**
     * Read the next data frame.
     * @throws IOException  On read error
     * @throws FrameDecodeException On frame decoding error
     */
    public void readFrame() throws IOException, FrameDecodeException {
        boolean gotAFrame = false;
        int channel;
        int i;
        int mid, side;
        short frameCRC; /* the one we calculate from the input stream */
        //int x;
        
        /* init the CRC */
        frameCRC = 0;
        frameCRC = CRC16.update(headerWarmup[0], frameCRC);
        frameCRC = CRC16.update(headerWarmup[1], frameCRC);
        bitStream.resetReadCRC16(frameCRC);
        
        try {
            frame.header = new Header(bitStream, headerWarmup, streamInfo);
        } catch (BadHeaderException e) {
            frameListeners.processError("Found bad header: " + e);
            throw new FrameDecodeException("Bad Frame Header: " + e);
        }
        //if (state == DECODER_SEARCH_FOR_FRAME_SYNC) return false;
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
                readSubframe(channel, bps);
            } catch (IOException e) {
                frameListeners.processError("ReadSubframe: " + e);
                throw e;
            }
        }
        readZeroPadding();
        
        // Read the frame CRC-16 from the footer and check
        frameCRC = bitStream.getReadCRC16();
        frame.setCRC((short)bitStream.readRawUInt(FRAME_FOOTER_CRC_LEN));
        if (frameCRC == frame.getCRC()) {
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
                    mid <<= 1;
                    mid |= (side & 1); /* i.e. if 'side' is odd... */                    
                    channelData[0].getOutput()[i] = (mid + side) >> 1;
                    channelData[1].getOutput()[i] = (mid - side) >> 1;
                }
            //System.exit(1);
            break;
            default :
                break;
            }
            
            gotAFrame = true;
        } else {
            // Bad frame, emit error and zero the output signal
            frameListeners.processError("CRC Error: " + Integer.toHexString((frameCRC & 0xffff)) + " vs " + Integer.toHexString((frame.getCRC() & 0xffff)));
            for (channel = 0; channel < frame.header.channels; channel++) {
                for (int j = 0; j < frame.header.blockSize; j++)
                    channelData[channel].getOutput()[j] = 0;
            }
        }
        
        // put the latest values into the public section of the decoder instance
        channels = frame.header.channels;
        
        //samplesDecoded = frame.header.sampleNumber + frame.header.blockSize;
        samplesDecoded += frame.header.blockSize;
        //System.out.println(samplesDecoded+" "+frame.header.sampleNumber + " "+frame.header.blockSize);
        
        //state = DECODER_SEARCH_FOR_FRAME_SYNC;
        //return;
    }
    
    private void readSubframe(int channel, int bps) throws IOException, FrameDecodeException {
        int x;
        
        x = bitStream.readRawUInt(8); /* MAGIC NUMBER */
        
        boolean haveWastedBits = ((x & 1) != 0);
        x &= 0xfe;
        
        int wastedBits = 0;
        if (haveWastedBits) {
            wastedBits = bitStream.readUnaryUnsigned() + 1;
            bps -= wastedBits;
        }
        
        // Lots of magic numbers here
        if ((x & 0x80) != 0) {
            frameListeners.processError("ReadSubframe LOST_SYNC: " + Integer.toHexString(x & 0xff));
            //state = DECODER_SEARCH_FOR_FRAME_SYNC;
            throw new FrameDecodeException("ReadSubframe LOST_SYNC: " + Integer.toHexString(x & 0xff));
            //return true;
        } else if (x == 0) {
            frame.subframes[channel] = new ChannelConstant(bitStream, frame.header, channelData[channel], bps, wastedBits);
        } else if (x == 2) {
            frame.subframes[channel] = new ChannelVerbatim(bitStream, frame.header, channelData[channel], bps, wastedBits);
        } else if (x < 16) {
            //state = DECODER_UNPARSEABLE_STREAM;
            throw new FrameDecodeException("ReadSubframe Bad Subframe Type: " + Integer.toHexString(x & 0xff));
        } else if (x <= 24) {
            //FLACSubframe_Fixed subframe = read_subframe_fixed_(channel, bps, (x >> 1) & 7);
            frame.subframes[channel] = new ChannelFixed(bitStream, frame.header, channelData[channel], bps, wastedBits, (x >> 1) & 7);
        } else if (x < 64) {
            //state = DECODER_UNPARSEABLE_STREAM;
            throw new FrameDecodeException("ReadSubframe Bad Subframe Type: " + Integer.toHexString(x & 0xff));
        } else {
            frame.subframes[channel] = new ChannelLPC(bitStream, frame.header, channelData[channel], bps, wastedBits, ((x >> 1) & 31) + 1);
        }
        if (haveWastedBits) {
            int i;
            x = frame.subframes[channel].getWastedBits();
            for (i = 0; i < frame.header.blockSize; i++)
                channelData[channel].getOutput()[i] <<= x;
        }
    }
    
    private void readZeroPadding() throws IOException, FrameDecodeException {
        if (!bitStream.isConsumedByteAligned()) {
            int zero = bitStream.readRawUInt(bitStream.bitsLeftForByteAlignment());
            if (zero != 0) {
                frameListeners.processError("ZeroPaddingError: " + Integer.toHexString(zero));
                //state = DECODER_SEARCH_FOR_FRAME_SYNC;
                throw new FrameDecodeException("ZeroPaddingError: " + Integer.toHexString(zero));
            }
        }
    }
    
    /**
     * Get the number of samples decoded.
     * @return Returns the samples Decoded.
     */
    public long getSamplesDecoded() {
        return samplesDecoded;
    }
    /**
     * @return Returns the number of bad frames decoded.
     */
    public int getBadFrames() {
        return badFrames;
    }
    /**
     * @return Returns true if end-of-file.
     */
    public boolean isEOF() {
        return eof;
    }
}
