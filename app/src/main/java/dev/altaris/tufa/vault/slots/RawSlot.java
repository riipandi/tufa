package dev.altaris.tufa.vault.slots;

import dev.altaris.tufa.crypto.CryptParameters;

import java.util.UUID;

public class RawSlot extends Slot {
    public RawSlot() {
        super();
    }

    protected RawSlot(UUID uuid, byte[] key, CryptParameters keyParams) {
        super(uuid, key, keyParams);
    }

    @Override
    public byte getType() {
        return TYPE_RAW;
    }
}
