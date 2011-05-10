package org.kc7bfi.jflac;

import junit.framework.TestCase;

import java.io.File;

/**
 * Abstract base class for test cases.
 *
 * @author <a href="jason@zenplex.com">Jason van Zyl</a>
 */
public abstract class AbstractTestCase extends TestCase {
    /** 
     * Basedir for all file I/O. Important when running tests from
     * the reactor.
     */
    public String basedir = System.getProperty("basedir");
    
    /**
     * Constructor.
     */
    protected AbstractTestCase(String testName) {
        super(testName);
    }

    protected AbstractTestCase() {
    }

    /**
     * Get test input file.
     *
     * @param path Path to test input file.
     * @return file relative to basedir
     */
    public String getTestFile(String path) {
        return new File(basedir,path).getAbsolutePath();
    }
}
