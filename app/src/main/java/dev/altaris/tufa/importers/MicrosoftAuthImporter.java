package dev.altaris.tufa.importers;

import android.content.Context;
import android.database.Cursor;

import dev.altaris.tufa.encoding.Base32;
import dev.altaris.tufa.encoding.Base64;
import dev.altaris.tufa.encoding.EncodingException;
import dev.altaris.tufa.otp.OtpInfo;
import dev.altaris.tufa.otp.OtpInfoException;
import dev.altaris.tufa.otp.TotpInfo;
import dev.altaris.tufa.vault.VaultEntry;

import java.util.List;

public class MicrosoftAuthImporter extends DatabaseImporter {
    private static final String _subPath = "databases/PhoneFactor";
    private static final String _pkgName = "com.azure.authenticator";

    private static final int TYPE_TOTP = 0;
    private static final int TYPE_MICROSOFT = 1;

    public MicrosoftAuthImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        return _pkgName;
    }

    @Override
    protected String getAppSubPath() {
        return _subPath;
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        SqlImporterHelper helper = new SqlImporterHelper(getContext());
        List<Entry> entries = helper.read(Entry.class, reader.getStream(), "accounts");
        return new State(entries);
    }

    public static class State extends DatabaseImporter.State {
        private List<Entry> _entries;

        private State(List<Entry> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (Entry sqlEntry : _entries) {
                try {
                    int type = sqlEntry.getType();
                    if (type == TYPE_TOTP || type == TYPE_MICROSOFT) {
                        VaultEntry entry = convertEntry(sqlEntry);
                        result.addEntry(entry);
                    }
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static VaultEntry convertEntry(Entry entry) throws DatabaseImporterEntryException {
            try {
                byte[] secret;
                int digits = 6;

                switch (entry.getType()) {
                    case TYPE_TOTP:
                        secret = Base32.decode(entry.getSecret());
                        break;
                    case TYPE_MICROSOFT:
                        digits = 8;
                        secret = Base64.decode(entry.getSecret());
                        break;
                    default:
                        throw new DatabaseImporterEntryException(String.format("Unsupported OTP type: %d", entry.getType()), entry.toString());
                }

                OtpInfo info = new TotpInfo(secret, "SHA1", digits, 30);
                return new VaultEntry(info, entry.getUserName(), entry.getIssuer());
            } catch (EncodingException | OtpInfoException e) {
                throw new DatabaseImporterEntryException(e, entry.toString());
            }
        }
    }

    private static class Entry extends SqlImporterHelper.Entry {
        private int _type;
        private String _secret;
        private String _issuer;
        private String _userName;

        public Entry(Cursor cursor) {
            super(cursor);
            _type = SqlImporterHelper.getInt(cursor, "account_type");
            _secret = SqlImporterHelper.getString(cursor, "oath_secret_key");
            _issuer = SqlImporterHelper.getString(cursor, "name");
            _userName = SqlImporterHelper.getString(cursor, "username");
        }

        public int getType() {
            return _type;
        }

        public String getSecret() {
            return _secret;
        }

        public String getIssuer() {
            return _issuer;
        }

        public String getUserName() {
            return _userName;
        }
    }
}
