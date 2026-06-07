package eu.forgeops.infra.vault;

import eu.forgeops.domain.vault.VaultEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AesGcmVaultEncryptor implements VaultEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final byte[] keyBytes;

    public AesGcmVaultEncryptor(@Value("${forgeops.vault.encryption-key:change-me-32-chars-secret-key!!}") String encryptionKey) {
        byte[] raw = encryptionKey.getBytes();
        this.keyBytes = Arrays.copyOf(raw, 32); // always 256-bit
    }

    @Override
    public byte[] encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
            byte[] result = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, result, GCM_IV_LENGTH, ciphertext.length);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(byte[] ciphertextWithIv) {
        try {
            byte[] iv = Arrays.copyOfRange(ciphertextWithIv, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(ciphertextWithIv, GCM_IV_LENGTH, ciphertextWithIv.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), spec);

            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
