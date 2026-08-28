package dev.watchnest.plannerapp.cms.api;

public final class CmsDemoAccountException extends RuntimeException {

    public static final String MESSAGE =
            "This is a demonstration account. The change was not applied.";

    public CmsDemoAccountException() {
        super(MESSAGE);
    }
}
