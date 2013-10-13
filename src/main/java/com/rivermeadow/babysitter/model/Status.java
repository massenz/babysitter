package com.rivermeadow.babysitter.model;

/**
 * TODO: enter class description here
 *
 * @author marco
 *
 */
public class Status {
    public enum Code {
        SUCCESS,
        FAILURE
    }

    Code statusCode;
    String message;

    /**
     * Required for JSON serialization, do not use
     */
    Status() {
    }

    /**
     * Do not use directly, prefer the factory methods
     *
     * @param statusCode
     * @param message
     */
    Status(Code statusCode, String message) {
        this.message = message;
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public Code getStatusCode() {
        return statusCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Status status = (Status) o;

        if (message != null ? !message.equals(status.message) : status.message != null)
            return false;
        if (statusCode != status.statusCode) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = statusCode != null ? statusCode.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    /**
     * Factory method to create an error status message
     *
     * @param detail more information about the error
     * @return a newly created error Status
     */
    public static Status createErrorStatus(String detail) {
        return new Status(Code.FAILURE, detail);
    }

    /**
     * Factory method for a successful return code
     *
     * @param detail more information, if necessary: can be @code{null}, in which case simply
     *               "Ok" will be used
     * @return a newly created success Status
     */
    public static Status createStatus(String detail) {
        if (detail == null) {
            detail = "Ok";
        }
        return new Status(Code.SUCCESS, detail);
    }
}
