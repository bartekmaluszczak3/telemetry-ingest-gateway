package org.example.gateway.exception;

public class DeviceAuthException extends RuntimeException{
    public DeviceAuthException(String message){
        super(message);
    }
}
