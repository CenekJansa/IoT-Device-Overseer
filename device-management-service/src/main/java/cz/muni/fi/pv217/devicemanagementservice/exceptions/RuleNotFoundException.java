package cz.muni.fi.pv217.devicemanagementservice.exceptions;

public class RuleNotFoundException extends RuntimeException {
    public RuleNotFoundException(String message) {
        super(message);
    }
}
