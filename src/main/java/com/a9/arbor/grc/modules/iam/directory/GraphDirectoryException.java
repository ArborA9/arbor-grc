package com.a9.arbor.grc.modules.iam.directory;

/** Graph token or search failed. Callers must fail open (empty directory), never SSO login. */
public class GraphDirectoryException extends RuntimeException {
    public GraphDirectoryException(String message) {
        super(message);
    }

    public GraphDirectoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
