package dev.altaris.tufa.vault;

import dev.altaris.tufa.encoding.EncodingException;
import dev.altaris.tufa.otp.OtpInfoException;
import dev.altaris.tufa.util.UUIDMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Vault {
    private static final int VERSION = 1;
    private UUIDMap<VaultEntry> _entries = new UUIDMap<>();

    public JSONObject toJson() {
        try {
            JSONArray array = new JSONArray();
            for (VaultEntry e : _entries) {
                array.put(e.toJson());
            }

            JSONObject obj = new JSONObject();
            obj.put("version", VERSION);
            obj.put("entries", array);
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Vault fromJson(JSONObject obj) throws VaultException {
        Vault vault = new Vault();
        UUIDMap<VaultEntry> entries = vault.getEntries();

        try {
            int ver = obj.getInt("version");
            if (ver != VERSION) {
                throw new VaultException("Unsupported version");
            }

            JSONArray array = obj.getJSONArray("entries");
            for (int i = 0; i < array.length(); i++) {
                VaultEntry entry = VaultEntry.fromJson(array.getJSONObject(i));
                entries.add(entry);
            }
        } catch (EncodingException | OtpInfoException | JSONException e) {
            throw new VaultException(e);
        }

        return vault;
    }

    public UUIDMap<VaultEntry> getEntries() {
        return _entries;
    }
}
