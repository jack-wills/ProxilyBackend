package org.videoApp.backend;

public class UnauthorisedException extends Exception {
    public UnauthorisedException() {
        super("You are not authorized");
    }
}
