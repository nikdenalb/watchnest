package dev.watchnest.identity.support;

import dev.watchnest.identity.port.PasswordHasher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class RecordingPasswordHasher implements PasswordHasher {

    private final List<String> hashedInputs = new ArrayList<>();

    @Override
    public String hash(String rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        hashedInputs.add(rawPassword);
        return "sha256:" + sha256(rawPassword);
    }

    @Override
    public boolean verify(String rawPassword, String passwordHash) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        Objects.requireNonNull(passwordHash, "passwordHash");
        return passwordHash.equals("sha256:" + sha256(rawPassword));
    }

    public List<String> hashedInputs() {
        return List.copyOf(hashedInputs);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
