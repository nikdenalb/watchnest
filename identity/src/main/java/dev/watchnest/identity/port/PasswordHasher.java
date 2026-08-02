package dev.watchnest.identity.port;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean verify(String rawPassword, String passwordHash);
}
