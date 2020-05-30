package dev.altaris.tufa.helpers.comparators;

import dev.altaris.tufa.vault.VaultEntry;

import java.util.Comparator;

public class IssuerNameComparator implements Comparator<VaultEntry> {
    @Override
    public int compare(VaultEntry a, VaultEntry b) {
        return a.getIssuer().compareToIgnoreCase(b.getIssuer());
    }
}
