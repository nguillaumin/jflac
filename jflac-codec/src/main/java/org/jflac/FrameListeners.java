/*
 * Created on Jun 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.jflac;

import java.util.HashSet;
import java.util.Iterator;

import org.jflac.frame.Frame;
import org.jflac.metadata.Metadata;


/**
 * Class to handle frame listeners.
 * @author kc7bfi
 */
class FrameListeners implements FrameListener {
    private HashSet frameListeners = new HashSet();
    
    /**
     * Add a frame listener.
     * @param listener  The frame listener to add
     */
    public void addFrameListener(FrameListener listener) {
        synchronized (frameListeners) {
            frameListeners.add(listener);
        }
    }
    
    /**
     * Remove a frame listener.
     * @param listener  The frame listener to remove
     */
    public void removeFrameListener(FrameListener listener) {
        synchronized (frameListeners) {
            frameListeners.remove(listener);
        }
    }
    
    /**
     * Process metadata records.
     * @param metadata the metadata block
     * @see org.jflac.FrameListener#processMetadata(org.jflac.metadata.MetadataBase)
     */
    public void processMetadata(Metadata metadata) {
        synchronized (frameListeners) {
            Iterator it = frameListeners.iterator();
            while (it.hasNext()) {
                FrameListener listener = (FrameListener)it.next();
                listener.processMetadata(metadata);
            }
        }
    }
    
    /**
     * Process data frames.
     * @param frame the data frame
     * @see org.jflac.FrameListener#processFrame(org.jflac.frame.Frame)
     */
    public void processFrame(Frame frame) {
        synchronized (frameListeners) {
            Iterator it = frameListeners.iterator();
            while (it.hasNext()) {
                FrameListener listener = (FrameListener)it.next();
                listener.processFrame(frame);
            }
        }
    }
   
    /**
     * Called for each frame error detected.
     * @param msg   The error message
     * @see org.jflac.FrameListener#processError(java.lang.String)
     */
    public void processError(String msg) {
        synchronized (frameListeners) {
            Iterator it = frameListeners.iterator();
            while (it.hasNext()) {
                FrameListener listener = (FrameListener)it.next();
                listener.processError(msg);
            }
        }
    }

}
