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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.kc7bfi.jflac.Constants;
import org.kc7bfi.jflac.StreamDecoder;
import org.kc7bfi.jflac.metadata.SeekPoint;
import org.kc7bfi.jflac.metadata.SeekTable;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.OutputBitStream;


/**
 * Assemble several FLAC files into one album.
 * @author kc7bfi
 */
public class FlacPacker extends JFrame {
    
    private JTextArea textArea = new JTextArea(16, 50);
    private JButton addButton = new JButton("Add Files");
    private JButton makeButton = new JButton("Pack FLAC");
    
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
                try {
                    packFlac();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
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
    
    private File getOutputFile() {
        JFileChooser chooser = new JFileChooser();
        ExtensionFileFilter filter = new ExtensionFileFilter();
        filter.addExtension("flac");
        filter.setDescription("FLAC files");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal != JFileChooser.APPROVE_OPTION) return null;
        File file = chooser.getSelectedFile();
        return file;
    }
    
    private SeekTable makeSeekTable() {
        long lastSampleNumber = 0;
        long lastStreamOffset = 0;
        
        // process selected files
        for (int i = 0; i < flacFiles.size(); i++) {
            File file = (File)flacFiles.get(i);
            try {
                System.out.print("SeekTable select " + i);
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
            System.out.print("SeekTable build " + i);
            points[i] = aFile.seekPoint;
        }
        
        return new SeekTable(points);
    }
    
    private void packFlac() throws IOException {
        // get output file
        File outFile = getOutputFile();
        if (outFile == null) return;
        OutputBitStream os = null;
        try {
            os = new OutputBitStream(new FileOutputStream(outFile));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return;
        }
        
        // get seek table
        SeekTable seekTable = makeSeekTable();
        if (masterStreamInfo == null) return;
        
        // output StreamInfo
        System.out.println("Write header");
        os.writeByteBlock(Constants.STREAM_SYNC_STRING, Constants.STREAM_SYNC_STRING.length);
        System.out.println("Write SI");
        masterStreamInfo.write(os, false);
        System.out.println("Rest");
        // output SeekTable
        
        // generate output file
        for (int i = 0; i < albumFiles.size(); i++) {
            AlbumFile aFile = (AlbumFile)albumFiles.get(i);
            System.out.println("Process file " + i + ": " + aFile.file);
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
