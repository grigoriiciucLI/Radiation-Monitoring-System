package service;

/** Thrown when a service-level rule is violated (e.g. null entity). */
public class ServiceException extends RuntimeException {
    public ServiceException(String message) { super(message); }
}
