package dev.altaris.tufa.importers;

import android.content.Context;

import dev.altaris.tufa.encoding.EncodingException;
import dev.altaris.tufa.otp.OtpInfoException;
import dev.altaris.tufa.vault.VaultEntry;
import dev.altaris.tufa.vault.VaultFile;
import dev.altaris.tufa.vault.VaultFileCredentials;
import dev.altaris.tufa.vault.VaultFileException;
import dev.altaris.tufa.vault.slots.SlotList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class TufaImporter extends DatabaseImporter {

    public TufaImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getAppSubPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        try {
            byte[] bytes = reader.readAll();
            VaultFile file = VaultFile.fromBytes(bytes);
            if (file.isEncrypted()) {
                return new EncryptedState(file);
            }
            return new DecryptedState(file.getContent());
        } catch (VaultFileException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class EncryptedState extends State {
        private VaultFile _file;

        private EncryptedState(VaultFile file) {
            super(true);
            _file = file;
        }

        public SlotList getSlots() {
            return _file.getHeader().getSlots();
        }

        public State decrypt(VaultFileCredentials creds) throws VaultFileException {
            JSONObject obj = _file.getContent(creds);
            return new DecryptedState(obj);
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {

        }
    }

    public static class DecryptedState extends State {
        private JSONObject _obj;

        private DecryptedState(JSONObject obj) {
            super(false);
            _obj = obj;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            Result result = new Result();

            try {
                JSONArray array = _obj.getJSONArray("entries");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject entryObj = array.getJSONObject(i);
                    try {
                        VaultEntry entry = convertEntry(entryObj);
                        result.addEntry(entry);
                    } catch (DatabaseImporterEntryException e) {
                        result.addError(e);
                    }
                }
            } catch (JSONException e) {
                throw new DatabaseImporterException(e);
            }

            return result;
        }

        private static VaultEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException {
            try {
                return VaultEntry.fromJson(obj);
            } catch (JSONException | OtpInfoException | EncodingException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }
}
