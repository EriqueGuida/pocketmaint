package br.com.pocketmaint.exception;

public class InvalidOrderStateTransitionException extends RuntimeException {

    public InvalidOrderStateTransitionException(String message) {
        super(message);
    }
}
