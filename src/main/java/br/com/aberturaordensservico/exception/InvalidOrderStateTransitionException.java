package br.com.aberturaordensservico.exception;

public class InvalidOrderStateTransitionException extends RuntimeException {

    public InvalidOrderStateTransitionException(String message) {
        super(message);
    }
}
