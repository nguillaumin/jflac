/*
 * Created on Apr 3, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.io;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.kc7bfi.jflac.ChannelData;
import org.kc7bfi.jflac.Constants;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.metadata.StreamInfo;
/**
 * @author kc7bfi
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class WavWriter {
    private long totalSamples;
    private int channels;
    private int bps;
    private int sampleRate;
    
    private byte[] s8buffer = new byte[Constants.MAX_BLOCK_SIZE * Constants.MAX_CHANNELS * 4]; /* WATCHOUT: can be up to 2 megs */
    private int samplesProcessed = 0;
    private int frameCounter = 0;
    
    private boolean needsFixup = false;
    private long riffOffset;
    private long dataOffset;
    
    private DataOutput os;
    private LittleEndianDataOutput osLE;
    
    public WavWriter(DataOutput os, StreamInfo streamInfo) {
        this.os = os;
        this.osLE = new LittleEndianDataOutput(os);
        this.totalSamples = streamInfo.totalSamples;
        this.channels = streamInfo.channels;
        this.bps = streamInfo.bitsPerSample;
        this.sampleRate = streamInfo.sampleRate;
    }
    
    public WavWriter(OutputStream os, StreamInfo streamInfo) {
        this.os = new DataOutputStream(os);
        this.osLE = new LittleEndianDataOutput(this.os);
        this.totalSamples = streamInfo.totalSamples;
        this.channels = streamInfo.channels;
        this.bps = streamInfo.bitsPerSample;
        this.sampleRate = streamInfo.sampleRate;
    }
    
    public void writeHeader() throws IOException {
        long dataSize = totalSamples * channels * ((bps + 7) / 8);
        System.out.println("totalSamples="+totalSamples+" channels="+channels+" bps="+bps);
        if (totalSamples == 0) {
            if (!(os instanceof RandomAccessFile)) throw new IOException("Cannot seek in output stream");
            needsFixup = true;
        }
        //if (dataSize >= 0xFFFFFFDC) throw new IOException("ERROR: stream is too big to fit in a single file chunk (Datasize="+dataSize+")");

        os.write("RIFF".getBytes());
        if (needsFixup) riffOffset = ((RandomAccessFile)os).getFilePointer();

        osLE.writeInt((int)dataSize + 36); // filesize-8
        os.write("WAVEfmt ".getBytes());
        os.write(new byte[] {0x10, 0x00, 0x00, 0x00}); // chunk size = 16
        os.write(new byte[] {0x01, 0x00}); // compression code == 1
        osLE.writeShort(channels);
        osLE.writeInt(sampleRate);
        osLE.writeInt(sampleRate * channels * ((bps + 7) / 8)); // or is it (sample_rate*channels*bps) / 8
        osLE.writeShort(channels * ((bps + 7) / 8)); // block align
        osLE.writeShort(bps); // bits per sample
        os.write("data".getBytes());
        if (needsFixup) dataOffset = ((RandomAccessFile)os).getFilePointer();
        
        osLE.writeInt((int)dataSize); // data size
    }

    public void writeFrame(Frame frame, ChannelData[] channelData) throws IOException {
        boolean isUnsignedSamples = (bps <= 8);
        int wideSamples = frame.header.blockSize;
        int wideSample, sample, channel, b;

        if (wideSamples > 0) {
            samplesProcessed += wideSamples;
            frameCounter++;
            if (bps == 8) {
                if (isUnsignedSamples) {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++) {
                            //System.out.print("("+(int)((byte)(channelData[channel].output[wideSample] + 0x80))+")");
                            s8buffer[sample++] = (byte)(channelData[channel].output[wideSample] + 0x80);
                        }
                } else {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++)
                            s8buffer[sample++] = (byte) (channelData[channel].output[wideSample]);
                }
                os.write(s8buffer, 0, sample);
            } else if (bps == 16) {
                if (isUnsignedSamples) {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++) {
                            short val = (short)(channelData[channel].output[wideSample] + 0x8000);
                            s8buffer[sample++] = (byte) ((val >> 8) & 0xff);
                            s8buffer[sample++] = (byte) (val & 0xff);
                        }
                } else {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++) {
                            short val = (short)(channelData[channel].output[wideSample]);
                            s8buffer[sample++] = (byte) ((val >> 8) & 0xff);
                            s8buffer[sample++] = (byte) (val & 0xff);
                        }
                }
                os.write(s8buffer, 0, sample);
            } else if (bps == 24) {
                if (isUnsignedSamples) {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++) {
                            short val = (short)(channelData[channel].output[wideSample] + 0x800000);
                            s8buffer[sample++] = (byte) ((val >> 16) & 0xff);
                            s8buffer[sample++] = (byte) ((val >> 8) & 0xff);
                            s8buffer[sample++] = (byte) (val & 0xff);
                        }
                } else {
                    for (sample = wideSample = 0; wideSample < wideSamples; wideSample++)
                        for (channel = 0; channel < channels; channel++) {
                            short val = (short)(channelData[channel].output[wideSample]);
                            s8buffer[sample++] = (byte) ((val >> 16) & 0xff);
                            s8buffer[sample++] = (byte) ((val >> 8) & 0xff);
                            s8buffer[sample++] = (byte) (val & 0xff);
                        }
                }
                os.write(s8buffer, 0, sample);
            }
        }
    }
}

