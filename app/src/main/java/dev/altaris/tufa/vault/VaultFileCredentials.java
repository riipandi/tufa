package dev.altaris.tufa.vault;

import dev.altaris.tufa.crypto.CryptParameters;
import dev.altaris.tufa.crypto.CryptResult;
import dev.altaris.tufa.crypto.MasterKey;
import dev.altaris.tufa.crypto.MasterKeyException;
import dev.altaris.tufa.vault.slots.SlotList;

import java.io.Serializable;

public class VaultFileCredentials implements Serializable {
    private MasterKey _key;
    private SlotList _slots;

    public VaultFileCredentials() {
        _key = MasterKey.generate();
        _slots = new SlotList();
    }

    public VaultFileCredentials(MasterKey key, SlotList slots) {
        _key = key;
        _slots = slots;
    }

    public CryptResult encrypt(byte[] bytes) throws MasterKeyException {
        return _key.encrypt(bytes);
    }

    public CryptResult decrypt(byte[] bytes, CryptParameters params) throws MasterKeyException {
        return _key.decrypt(bytes, params);
    }

    public MasterKey getKey() {
        return _key;
    }

    public SlotList getSlots() {
        return _slots;
    }
}
