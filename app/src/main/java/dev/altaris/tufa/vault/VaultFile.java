package dev.altaris.tufa.vault;

import dev.altaris.tufa.crypto.CryptParameters;
import dev.altaris.tufa.crypto.CryptResult;
import dev.altaris.tufa.crypto.MasterKeyException;
import dev.altaris.tufa.encoding.Base64;
import dev.altaris.tufa.encoding.EncodingException;
import dev.altaris.tufa.vault.slots.SlotList;
import dev.altaris.tufa.vault.slots.SlotListException;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class VaultFile {
    public static final byte VERSION = 1;

    private Object _content;
    private Header _header;

    public VaultFile() {

    }

    private VaultFile(Object content, Header header) {
        _content = content;
        _header = header;
    }

    public Header getHeader() {
        return _header;
    }

    public boolean isEncrypted() {
        return !_header.isEmpty();
    }

    public JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("version", VERSION);
            obj.put("header", _header.toJson());
            obj.put("db", _content);
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] toBytes() {
        JSONObject obj = toJson();

        try {
            String string = obj.toString(4);
            return string.getBytes(StandardCharsets.UTF_8);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static VaultFile fromJson(JSONObject obj) throws VaultFileException {
        try {
            if (obj.getInt("version") > VERSION) {
                throw new VaultFileException("unsupported version");
            }

            Header header = Header.fromJson(obj.getJSONObject("header"));
            if (!header.isEmpty()) {
                return new VaultFile(obj.getString("db"), header);
            }

            return new VaultFile(obj.getJSONObject("db"), header);
        } catch (JSONException e) {
            throw new VaultFileException(e);
        }
    }

    public static VaultFile fromBytes(byte[] data) throws VaultFileException {
        try {
            JSONObject obj = new JSONObject(new String(data, StandardCharsets.UTF_8));
            return VaultFile.fromJson(obj);
        } catch (JSONException e) {
            throw new VaultFileException(e);
        }
    }

    public JSONObject getContent() {
        return (JSONObject) _content;
    }

    public JSONObject getContent(VaultFileCredentials creds) throws VaultFileException {
        try {
            byte[] bytes = Base64.decode((String) _content);
            CryptResult result = creds.decrypt(bytes, _header.getParams());
            return new JSONObject(new String(result.getData(), StandardCharsets.UTF_8));
        } catch (MasterKeyException | JSONException | EncodingException e) {
            throw new VaultFileException(e);
        }
    }

    public void setContent(JSONObject obj) {
        _content = obj;
        _header = new Header(null, null);
    }

    public void setContent(JSONObject obj, VaultFileCredentials creds) throws VaultFileException {
        try {
            String string = obj.toString(4);
            byte[] vaultBytes = string.getBytes(StandardCharsets.UTF_8);

            CryptResult result = creds.encrypt(vaultBytes);
            _content = Base64.encode(result.getData());
            _header = new Header(creds.getSlots(), result.getParams());
        } catch (MasterKeyException | JSONException e) {
            throw new VaultFileException(e);
        }
    }

    public static class Header {
        private SlotList _slots;
        private CryptParameters _params;

        public Header(SlotList slots, CryptParameters params) {
            _slots = slots;
            _params = params;
        }

        public static Header fromJson(JSONObject obj) throws VaultFileException {
            if (obj.isNull("slots") && obj.isNull("params")) {
                return new Header(null, null);
            }

            try {
                SlotList slots = SlotList.fromJson(obj.getJSONArray("slots"));
                CryptParameters params = CryptParameters.fromJson(obj.getJSONObject("params"));
                return new Header(slots, params);
            } catch (SlotListException | JSONException | EncodingException e) {
                throw new VaultFileException(e);
            }
        }

        public JSONObject toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("slots", _slots != null ? _slots.toJson() : JSONObject.NULL);
                obj.put("params", _params != null ? _params.toJson() : JSONObject.NULL);
                return obj;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        public SlotList getSlots() {
            return _slots;
        }

        public CryptParameters getParams() {
            return _params;
        }

        public boolean isEmpty() {
            return _slots == null && _params == null;
        }
    }
}
