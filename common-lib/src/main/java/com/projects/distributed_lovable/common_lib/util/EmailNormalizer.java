package com.projects.distributed_lovable.common_lib.util;

/**
 * Gmail (and Google Workspace) treat dots in the local part of an address as
 * insignificant, so "user.name@gmail.com" and "username@gmail.com" are the
 * same mailbox. Normalizing before matching keeps invite lookups working
 * regardless of which dotted variant someone used.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase();
        int atIndex = trimmed.indexOf('@');
        if (atIndex < 0) {
            return trimmed;
        }

        String localPart = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex + 1);

        if (domain.equals("gmail.com") || domain.equals("googlemail.com")) {
            localPart = localPart.replace(".", "");
        }

        return localPart + "@" + domain;
    }
}
