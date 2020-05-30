package dev.altaris.tufa.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;

import dev.altaris.tufa.TufaApplication;
import dev.altaris.tufa.CancelAction;
import dev.altaris.tufa.Preferences;
import dev.altaris.tufa.R;
import dev.altaris.tufa.crypto.KeyStoreHandle;
import dev.altaris.tufa.crypto.KeyStoreHandleException;
import dev.altaris.tufa.crypto.MasterKey;
import dev.altaris.tufa.helpers.BiometricsHelper;
import dev.altaris.tufa.helpers.EditTextHelper;
import dev.altaris.tufa.helpers.UiThreadExecutor;
import dev.altaris.tufa.ui.tasks.PasswordSlotDecryptTask;
import dev.altaris.tufa.vault.VaultFile;
import dev.altaris.tufa.vault.VaultFileCredentials;
import dev.altaris.tufa.vault.VaultManager;
import dev.altaris.tufa.vault.VaultManagerException;
import dev.altaris.tufa.vault.slots.BiometricSlot;
import dev.altaris.tufa.vault.slots.PasswordSlot;
import dev.altaris.tufa.vault.slots.Slot;
import dev.altaris.tufa.vault.slots.SlotException;
import dev.altaris.tufa.vault.slots.SlotIntegrityException;
import dev.altaris.tufa.vault.slots.SlotList;

import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class AuthActivity extends TufaActivity {
    private EditText _textPassword;

    private CancelAction _cancelAction;
    private SlotList _slots;

    private SecretKey _bioKey;
    private BiometricSlot _bioSlot;
    private BiometricPrompt _bioPrompt;

    private Preferences _prefs;
    private boolean _stateless;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _prefs = new Preferences(this);
        setContentView(R.layout.activity_auth);
        _textPassword = findViewById(R.id.text_password);
        LinearLayout boxBiometricInfo = findViewById(R.id.box_biometric_info);
        Button decryptButton = findViewById(R.id.button_decrypt);
        Button biometricsButton = findViewById(R.id.button_biometrics);

        _textPassword.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                decryptButton.performClick();
            }
            return false;
        });

        Intent intent = getIntent();
        _cancelAction = (CancelAction) intent.getSerializableExtra("cancelAction");
        _slots = (SlotList) intent.getSerializableExtra("slots");
        _stateless = _slots != null;
        if (!_stateless) {
            VaultFile vaultFile;
            try {
                vaultFile = getApp().loadVaultFile();
            } catch (VaultManagerException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.vault_load_error, e, (dialog, which) -> onBackPressed());
                return;
            }

            _slots = vaultFile.getHeader().getSlots();
        }

        // only show the biometric prompt if the api version is new enough, permission is granted, a scanner is found and a biometric slot is found
        if (_slots.has(BiometricSlot.class) && BiometricsHelper.isAvailable(this)) {
            boolean invalidated = false;

            try {
                // find a biometric slot with an id that matches an alias in the keystore
                for (BiometricSlot slot : _slots.findAll(BiometricSlot.class)) {
                    String id = slot.getUUID().toString();
                    KeyStoreHandle handle = new KeyStoreHandle();
                    if (handle.containsKey(id)) {
                        SecretKey key = handle.getKey(id);
                        // if 'key' is null, it was permanently invalidated
                        if (key == null) {
                            invalidated = true;
                            continue;
                        }

                        _bioSlot = slot;
                        _bioKey = key;
                        biometricsButton.setVisibility(View.VISIBLE);
                        invalidated = false;
                        break;
                    }
                }
            } catch (KeyStoreHandleException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.biometric_init_error, e);
            }

            // display a help message if a matching invalidated keystore entry was found
            if (invalidated) {
                boxBiometricInfo.setVisibility(View.VISIBLE);
            }
        }

        decryptButton.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            char[] password = EditTextHelper.getEditTextChars(_textPassword);
            List<PasswordSlot> slots = _slots.findAll(PasswordSlot.class);
            PasswordSlotDecryptTask.Params params = new PasswordSlotDecryptTask.Params(slots, password);
            new PasswordSlotDecryptTask(AuthActivity.this, new PasswordDerivationListener()).execute(params);
        });

        biometricsButton.setOnClickListener(v -> {
            showBiometricPrompt();
        });
    }

    private void selectPassword() {
        _textPassword.selectAll();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public void onBackPressed() {
        switch (_cancelAction) {
            case KILL:
                finishAffinity();
            case CLOSE:
                finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (_bioKey != null) {
            if (_prefs.isPasswordReminderNeeded()) {
                focusPasswordField();
            } else {
                showBiometricPrompt();
            }
        } else {
            focusPasswordField();
        }
    }

    @Override
    public void onAttachedToWindow() {
        if (_bioPrompt != null && _prefs.isPasswordReminderNeeded()) {
            showPasswordReminder();
        }
    }

    private void focusPasswordField() {
        _textPassword.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void showPasswordReminder() {
        View popupLayout = getLayoutInflater().inflate(R.layout.popup_password, null);
        PopupWindow popup = new PopupWindow(popupLayout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(false);
        popup.setOutsideTouchable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            popup.setElevation(5.0f);
        }
        _textPassword.post(() -> popup.showAsDropDown(_textPassword));
        _textPassword.postDelayed(popup::dismiss, 5000);
    }

    public void showBiometricPrompt() {
        Cipher cipher;
        try {
            cipher = _bioSlot.createDecryptCipher(_bioKey);
        } catch (SlotException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.biometric_init_error, e);
            return;
        }

        BiometricPrompt.CryptoObject cryptoObj = new BiometricPrompt.CryptoObject(cipher);
        _bioPrompt = new BiometricPrompt(this, new UiThreadExecutor(), new BiometricPromptListener());

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.authentication))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .setConfirmationRequired(false)
                .build();
        _bioPrompt.authenticate(info, cryptoObj);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (_bioPrompt != null) {
            _bioPrompt.cancelAuthentication();
        }
    }

    private void finish(MasterKey key, boolean isSlotRepaired) {
        VaultFileCredentials creds = new VaultFileCredentials(key, _slots);

        if (_stateless) {
            // send the master key back to the calling activity
            Intent intent = new Intent();
            intent.putExtra("creds", creds);
            setResult(RESULT_OK, intent);
        } else {
            try {
                TufaApplication app = getApp();
                VaultManager vault = app.initVaultManager(app.loadVaultFile(), creds);
                if (isSlotRepaired) {
                    vault.setCredentials(creds);
                    saveVault();
                }
            } catch (VaultManagerException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.decryption_corrupt_error, e);
                return;
            }

            setResult(RESULT_OK);
        }

        finish();
    }

    private class PasswordDerivationListener implements PasswordSlotDecryptTask.Callback {
        @Override
        public void onTaskFinished(PasswordSlotDecryptTask.Result result) {
            if (result != null) {
                // replace the old slot with the repaired one
                if (result.isSlotRepaired()) {
                    _slots.replace(result.getSlot());
                }

                if (result.getSlot().getType() == Slot.TYPE_DERIVED) {
                    _prefs.resetPasswordReminderTimestamp();
                }

                finish(result.getKey(), result.isSlotRepaired());
            } else {
                Dialogs.showSecureDialog(new AlertDialog.Builder(AuthActivity.this)
                        .setTitle(getString(R.string.unlock_vault_error))
                        .setMessage(getString(R.string.unlock_vault_error_description))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> selectPassword())
                        .create());
            }
        }
    }

    private class BiometricPromptListener extends BiometricPrompt.AuthenticationCallback {
        @Override
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            if (!BiometricsHelper.isCanceled(errorCode)) {
                Toast.makeText(AuthActivity.this, errString, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);

            MasterKey key;
            BiometricSlot slot = _slots.find(BiometricSlot.class);

            try {
                key = slot.getKey(result.getCryptoObject().getCipher());
            } catch (SlotException | SlotIntegrityException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(AuthActivity.this, R.string.biometric_decrypt_error, e);
                return;
            }

            finish(key, false);
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
        }
    }
}
