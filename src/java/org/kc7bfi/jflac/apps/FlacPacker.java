/*
 * Created on Jun 7, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.apps;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.kc7bfi.jflac.StreamDecoder;
import org.kc7bfi.jflac.metadata.SeekPoint;
import org.kc7bfi.jflac.metadata.StreamInfo;


/**
 * Assemble several FLAC files into one album.
 * @author kc7bfi
 */
public class FlacPacker extends JFrame {
    
    private JTextArea textArea = new JTextArea(16, 50);
    private JButton addButton = new JButton("Add Files");
    private JButton makeButton = new JButton("Make Album");
    
    private ArrayList flacFiles = new ArrayList();
    private ArrayList albumFiles = new ArrayList();
    private StreamInfo masterStreamInfo = null;
    
    /**
     * Constructor.
     * @param title     Frame title
     */
    public FlacPacker(String title) {
        super(title);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        this.getContentPane().setLayout(new BorderLayout());
        
        // text area
        this.getContentPane().add(textArea, BorderLayout.CENTER);
        
        // button pannel
        Panel buttonPanel = new Panel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(addButton);
        buttonPanel.add(makeButton);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        this.pack();
        
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addFilesToList();
            }
        });
        
        makeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                makeAlbum();
            }
        });
    }
    
    private void addFilesToList() {
        JFileChooser chooser = new JFileChooser();
        ExtensionFileFilter filter = new ExtensionFileFilter();
        filter.addExtension("flac");
        filter.setDescription("FLAC files");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal != JFileChooser.APPROVE_OPTION) return;
        File[] files = chooser.getSelectedFiles();
        for (int i = 0; i < files.length; i++) flacFiles.add(files[i]);
    }
    
    private void makeAlbum() {
        long lastSampleNumber = 0;
        long lastStreamOffset = 0;
        
        // process selected files
        for (int i = 0; i < flacFiles.size(); i++) {
            File file = (File)flacFiles.get(i);
            try {
                FileInputStream is = new FileInputStream(file);
                StreamDecoder decoder = new StreamDecoder(is);
                decoder.processMetadata();
                StreamInfo info = decoder.getStreamInfo();
                if (masterStreamInfo == null) masterStreamInfo = info;
                if (!info.compatiable(masterStreamInfo)) {
                    System.out.println("Bad StreamInfo " + file + ": " + info);
                    continue;
                }
                
                SeekPoint seekPoint = new SeekPoint(lastSampleNumber, lastStreamOffset, 0);
                AlbumFile aFile = new AlbumFile(file, seekPoint);
                albumFiles.add(aFile);
                lastSampleNumber += info.totalSamples;
                lastStreamOffset += file.length() - decoder.getBytesConsumed();
            } catch (FileNotFoundException e) {
                System.out.println("File " + file + ": " + e);
            } catch (IOException e) {
                System.out.println("File " + file + ": " + e);
            }
        }
        
        // make Seek Table
        SeekPoint[] points = new SeekPoint[albumFiles.size()];
        for (int i = 0; i < albumFiles.size(); i++) {
            AlbumFile aFile = (AlbumFile)albumFiles.get(i);
            points[i] = aFile.seekPoint;
        }
        
        // generate output file
        for (int i = 0; i < albumFiles.size(); i++) {
            AlbumFile aFile = (AlbumFile)albumFiles.get(i);
            try {
                FileInputStream is = new FileInputStream(aFile.file);
                StreamDecoder decoder = new StreamDecoder(is);
                decoder.processMetadata();
            } catch (FileNotFoundException e) {
                System.out.println("File " + aFile.file + ": " + e);
            } catch (IOException e) {
                System.out.println("File " + aFile.file + ": " + e);
            }
        }
    }
    
    /**
     * Main routine.
     * @param args  Command line arguments
     */
    public static void main(String[] args) {
        FlacPacker app = new FlacPacker("FLAC Album Maker");
        app.show(true);
    }
    
    private class AlbumFile {
        public File file;
        public SeekPoint seekPoint;
        
        public AlbumFile(File file, SeekPoint seekPoint) {
            this.file = file;
            this.seekPoint = seekPoint;
        }
    }
}
