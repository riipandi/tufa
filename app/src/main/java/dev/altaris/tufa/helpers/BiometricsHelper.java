package dev.altaris.tufa.helpers;

import android.content.Context;

import androidx.biometric.BiometricConstants;
import androidx.biometric.BiometricManager;

public class BiometricsHelper {
    private BiometricsHelper() {

    }

    public static BiometricManager getManager(Context context) {
        BiometricManager manager = BiometricManager.from(context);
        if (manager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            return manager;
        }
        return null;
    }

    public static boolean isCanceled(int errorCode) {
        return errorCode == BiometricConstants.ERROR_CANCELED
                || errorCode == BiometricConstants.ERROR_USER_CANCELED
                || errorCode == BiometricConstants.ERROR_NEGATIVE_BUTTON;
    }

    public static boolean isAvailable(Context context) {
        return getManager(context) != null;
    }
}
