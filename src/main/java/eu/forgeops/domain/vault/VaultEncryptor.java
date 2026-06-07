package eu.forgeops.domain.vault;

public interface VaultEncryptor {
    byte[] encrypt(String plaintext);
    String decrypt(byte[] ciphertext);
}
