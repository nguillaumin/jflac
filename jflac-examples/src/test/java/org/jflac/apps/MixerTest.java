package org.jflac.apps;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer.Info;

import org.junit.Assert;
import org.junit.Test;

/**
 * Simple test to check that a mixer is available,
 * mostly to debug Travis CI issues.
 * 
 * @author nicolas+github@guillaumin.me
 *
 */
public class MixerTest {

    @Test
    public void test() {
        Info[] infos = AudioSystem.getMixerInfo();
        
        Assert.assertTrue("No mixer available", infos.length > 0);
        
        System.out.println("Found " + infos.length + " mixers");
        
        for (Info info: infos) {
            System.out.println("Found mixer info: " + info.getName());
            System.out.println("\tDescription: " + info.getDescription());
            System.out.println("\tVendor: " + info.getVendor());
            System.out.println("\tVersion:" + info.getVersion());
        }
    }
    
}
