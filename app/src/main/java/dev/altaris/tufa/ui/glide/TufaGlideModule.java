package dev.altaris.tufa.ui.glide;

import android.content.Context;

import androidx.annotation.NonNull;

import dev.altaris.tufa.vault.VaultEntry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import java.nio.ByteBuffer;

@GlideModule
public class TufaGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.prepend(VaultEntry.class, ByteBuffer.class, new IconLoader.Factory());
    }
}
