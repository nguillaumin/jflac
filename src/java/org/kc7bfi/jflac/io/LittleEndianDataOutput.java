/*
 * Created on Apr 3, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.io;

import java.io.DataOutput;
import java.io.IOException;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LittleEndianDataOutput implements DataOutput {
    
    private DataOutput out;
    
    public LittleEndianDataOutput(DataOutput out) {
        this.out = out;
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeDouble(double)
     */
    public void writeDouble(double arg0) throws IOException {
        out.writeDouble(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeFloat(float)
     */
    public void writeFloat(float arg0) throws IOException {
        out.writeFloat(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#write(int)
     */
    public void write(int arg0) throws IOException {
        out.write(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeByte(int)
     */
    public void writeByte(int arg0) throws IOException {
        out.writeByte(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeChar(int)
     */
    public void writeChar(int arg0) throws IOException {
        out.writeChar(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeInt(int)
     */
    public void writeInt(int arg0) throws IOException {
        out.writeByte(arg0 & 0xff);
        out.writeByte((arg0 >> 8) & 0xff);
        out.writeByte((arg0 >> 16) & 0xff);
        out.writeByte((arg0 >> 24) & 0xff);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeShort(int)
     */
    public void writeShort(int arg0) throws IOException {
        out.writeByte(arg0 & 0xff);
        out.writeByte((arg0 >> 8) & 0xff);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeLong(long)
     */
    public void writeLong(long arg0) throws IOException {
        out.writeByte((int)arg0 & 0xff);
        out.writeByte((int)(arg0 >> 8) & 0xff);
        out.writeByte((int)(arg0 >> 16) & 0xff);
        out.writeByte((int)(arg0 >> 24) & 0xff);
        out.writeByte((int)(arg0 >> 32) & 0xff);
        out.writeByte((int)(arg0 >> 40) & 0xff);
        out.writeByte((int)(arg0 >> 48) & 0xff);
        out.writeByte((int)(arg0 >> 56) & 0xff);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeBoolean(boolean)
     */
    public void writeBoolean(boolean arg0) throws IOException {
        writeBoolean(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#write(byte[])
     */
    public void write(byte[] arg0) throws IOException {
        out.write(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#write(byte[], int, int)
     */
    public void write(byte[] arg0, int arg1, int arg2) throws IOException {
        out.write(arg0, arg1, arg2);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeBytes(java.lang.String)
     */
    public void writeBytes(String arg0) throws IOException {
        out.writeBytes(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeChars(java.lang.String)
     */
    public void writeChars(String arg0) throws IOException {
        out.writeChars(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeUTF(java.lang.String)
     */
    public void writeUTF(String arg0) throws IOException {
        out.writeUTF(arg0);
    }
}
