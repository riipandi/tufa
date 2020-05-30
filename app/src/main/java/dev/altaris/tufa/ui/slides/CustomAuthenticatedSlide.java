package dev.altaris.tufa.ui.slides;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;

import dev.altaris.tufa.R;
import dev.altaris.tufa.helpers.BiometricSlotInitializer;
import dev.altaris.tufa.helpers.BiometricsHelper;
import dev.altaris.tufa.helpers.EditTextHelper;
import dev.altaris.tufa.ui.Dialogs;
import dev.altaris.tufa.ui.IntroActivity;
import dev.altaris.tufa.ui.tasks.KeyDerivationTask;
import dev.altaris.tufa.vault.VaultFileCredentials;
import dev.altaris.tufa.vault.slots.BiometricSlot;
import dev.altaris.tufa.vault.slots.PasswordSlot;
import dev.altaris.tufa.vault.slots.Slot;
import dev.altaris.tufa.vault.slots.SlotException;
import com.github.appintro.SlidePolicy;
import com.github.appintro.SlideSelectionListener;
import com.google.android.material.snackbar.Snackbar;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class CustomAuthenticatedSlide extends Fragment implements SlidePolicy, SlideSelectionListener {
    private int _bgColor;
    private EditText _textPassword;
    private EditText _textPasswordConfirm;
    private CheckBox _checkPasswordVisibility;

    private int _cryptType;
    private VaultFileCredentials _creds;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_authenticated_slide, container, false);
        _textPassword = view.findViewById(R.id.text_password);
        _textPasswordConfirm = view.findViewById(R.id.text_password_confirm);
        _checkPasswordVisibility = view.findViewById(R.id.check_toggle_visibility);

        _checkPasswordVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                _textPassword.setTransformationMethod(null);
                _textPasswordConfirm.setTransformationMethod(null);
                _textPassword.clearFocus();
                _textPasswordConfirm.clearFocus();
            } else {
                _textPassword.setTransformationMethod(new PasswordTransformationMethod());
                _textPasswordConfirm.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        view.findViewById(R.id.main).setBackgroundColor(_bgColor);
        return view;
    }

    public int getCryptType() {
        return _cryptType;
    }

    public VaultFileCredentials getCredentials() {
        return _creds;
    }

    public void setBgColor(int color) {
        _bgColor = color;
    }

    private void showBiometricPrompt() {
        BiometricSlotInitializer initializer = new BiometricSlotInitializer(this, new BiometricsListener());
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.set_up_biometric))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .build();
        initializer.authenticate(info);
    }

    private void deriveKey() {
        PasswordSlot slot = new PasswordSlot();
        KeyDerivationTask.Params params = new KeyDerivationTask.Params(slot, EditTextHelper.getEditTextChars(_textPassword));
        new KeyDerivationTask(getContext(), new PasswordDerivationListener()).execute(params);
    }

    @Override
    public void onSlideSelected() {
        Intent intent = getActivity().getIntent();
        _cryptType = intent.getIntExtra("cryptType", CustomAuthenticationSlide.CRYPT_TYPE_INVALID);
        if (_cryptType != CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
            _creds = new VaultFileCredentials();
        }
    }

    @Override
    public void onSlideDeselected() {

    }

    @Override
    public boolean isPolicyRespected() {
        switch (_cryptType) {
            case CustomAuthenticationSlide.CRYPT_TYPE_NONE:
                return true;
            case CustomAuthenticationSlide.CRYPT_TYPE_BIOMETRIC:
                if (!_creds.getSlots().has(BiometricSlot.class)) {
                    return false;
                }
                // intentional fallthrough
            case CustomAuthenticationSlide.CRYPT_TYPE_PASS:
                if (EditTextHelper.areEditTextsEqual(_textPassword, _textPasswordConfirm)) {
                    return _creds.getSlots().has(PasswordSlot.class);
                }

                return false;
            default:
                return false;
        }
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        String message;
        if (!EditTextHelper.areEditTextsEqual(_textPassword, _textPasswordConfirm)) {
            message = getString(R.string.password_equality_error);

            View view = getView();
            if (view != null) {
                Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        } else if (_cryptType != CustomAuthenticationSlide.CRYPT_TYPE_BIOMETRIC) {
            deriveKey();
        } else if (!_creds.getSlots().has(BiometricSlot.class)) {
            showBiometricPrompt();
        }
    }

    private class PasswordDerivationListener implements KeyDerivationTask.Callback {
        @Override
        public void onTaskFinished(PasswordSlot slot, SecretKey key) {
            try {
                Cipher cipher = Slot.createEncryptCipher(key);
                slot.setKey(_creds.getKey(), cipher);
                _creds.getSlots().add(slot);
            } catch (SlotException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(getContext(), R.string.enable_encryption_error, e);
                return;
            }

            ((IntroActivity) getActivity()).goToNextSlide();
        }
    }

    private class BiometricsListener implements BiometricSlotInitializer.Listener {
        @Override
        public void onInitializeSlot(BiometricSlot slot, Cipher cipher) {
            try {
                slot.setKey(_creds.getKey(), cipher);
                _creds.getSlots().add(slot);
            } catch (SlotException e) {
                e.printStackTrace();
                onSlotInitializationFailed(0, e.toString());
                return;
            }

            deriveKey();
        }

        @Override
        public void onSlotInitializationFailed(int errorCode, @NonNull CharSequence errString) {
            if (!BiometricsHelper.isCanceled(errorCode)) {
                Dialogs.showErrorDialog(getContext(), R.string.encryption_enable_biometrics_error, errString);
            }
        }
    }
}
