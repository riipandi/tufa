package dev.altaris.tufa.ui.views;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import dev.altaris.tufa.R;
import dev.altaris.tufa.crypto.KeyStoreHandle;
import dev.altaris.tufa.crypto.KeyStoreHandleException;
import dev.altaris.tufa.vault.slots.BiometricSlot;
import dev.altaris.tufa.vault.slots.PasswordSlot;
import dev.altaris.tufa.vault.slots.RawSlot;
import dev.altaris.tufa.vault.slots.Slot;
import dev.altaris.tufa.helpers.BiometricsHelper;

public class SlotHolder extends RecyclerView.ViewHolder {
    private TextView _slotUsed;
    private TextView _slotName;
    private ImageView _slotImg;
    private LinearLayout _buttonEdit;
    private ImageView _buttonDelete;

    public SlotHolder(final View view) {
        super(view);
        _slotUsed = view.findViewById(R.id.text_slot_used);
        _slotName = view.findViewById(R.id.text_slot_name);
        _slotImg = view.findViewById(R.id.img_slot);
        _buttonEdit = view.findViewById(R.id.button_edit);
        _buttonDelete = view.findViewById(R.id.button_delete);
    }

    public void setData(Slot slot) {
        if (slot instanceof PasswordSlot) {
            _slotName.setText(R.string.password);
            _slotImg.setImageResource(R.drawable.ic_create_black_24dp);
        } else if (slot instanceof BiometricSlot) {
            _slotName.setText(R.string.authentication_method_biometrics);
            _slotImg.setImageResource(R.drawable.ic_fingerprint_black_24dp);
            if (BiometricsHelper.isAvailable(itemView.getContext())) {
                try {
                    KeyStoreHandle keyStore = new KeyStoreHandle();
                    if (keyStore.containsKey(slot.getUUID().toString())) {
                        _slotUsed.setVisibility(View.VISIBLE);
                    }
                } catch (KeyStoreHandleException e) { }
            }
        } else if (slot instanceof RawSlot) {
            _slotName.setText(R.string.authentication_method_raw);
            _slotImg.setImageResource(R.drawable.ic_vpn_key_black_24dp);
        } else {
            throw new RuntimeException(String.format("Unsupported Slot type: %s", slot.getClass()));
        }
    }

    public void setOnEditClickListener(View.OnClickListener listener) {
        _buttonEdit.setOnClickListener(listener);
    }

    public void setOnDeleteClickListener(View.OnClickListener listener) {
        _buttonDelete.setOnClickListener(listener);
    }
}
