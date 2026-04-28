package com.homekm.file;

import java.io.InputStream;

public interface AvScanner {

    /** Scan an input stream. Implementations must return a status without consuming the original stream beyond what's needed. */
    ScanResult scan(InputStream content);

    enum Status { CLEAN, INFECTED, ERROR }

    record ScanResult(Status status, String detail) {
        public static ScanResult clean() { return new ScanResult(Status.CLEAN, null); }
        public static ScanResult infected(String name) { return new ScanResult(Status.INFECTED, name); }
        public static ScanResult error(String detail) { return new ScanResult(Status.ERROR, detail); }
    }
}
