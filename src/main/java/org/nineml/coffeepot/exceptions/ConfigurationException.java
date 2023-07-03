package org.nineml.coffeepot.exceptions;

public class ConfigurationException extends RuntimeException {
    public static final int HELP = 0;
    public static final int BADARGS = 1;
    public static final int BADCONFIG = 2;
    public static final int NOINPUT = 3;
    public final int errorCode;

    public ConfigurationException(int code) {
        super();
        errorCode = code;
    }

    public ConfigurationException(int code, String message) {
        super(message);
        errorCode = code;
    }

    public static ConfigurationException help() {
        return new ConfigurationException(HELP);
    }

    public static ConfigurationException argsError() {
        return new ConfigurationException(BADARGS);
    }

    public static ConfigurationException noInput() {
        return new ConfigurationException(NOINPUT);
    }

    public static ConfigurationException configError(String message) {
        return new ConfigurationException(BADCONFIG, message);
    }

}
