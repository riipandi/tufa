package dev.altaris.tufa.vault.slots;

public class SlotException extends Exception {
    public SlotException(Throwable cause) {
        super(cause);
    }

    public SlotException(String message) {
        super(message);
    }
}
