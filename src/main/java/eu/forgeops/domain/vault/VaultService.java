package eu.forgeops.domain.vault;

import eu.forgeops.infra.persistence.SecretRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class VaultService {

    private final SecretRepository secretRepository;
    private final VaultEncryptor vaultEncryptor;

    @Transactional
    public Secret createSecret(String name, Secret.SecretType type, String plaintext, String description, UUID createdBy) {
        if (secretRepository.existsByName(name)) {
            throw new ResponseStatusException(CONFLICT, "Secret with name already exists: " + name);
        }
        Secret secret = Secret.builder()
            .name(name)
            .secretType(type)
            .encryptedValue(vaultEncryptor.encrypt(plaintext))
            .description(description)
            .createdBy(createdBy)
            .build();
        return secretRepository.save(secret);
    }

    @Transactional
    public Secret updateSecret(UUID id, String plaintext, String description, UUID updatedBy) {
        Secret secret = getById(id);
        if (plaintext != null && !plaintext.isBlank()) {
            secret.setEncryptedValue(vaultEncryptor.encrypt(plaintext));
        }
        if (description != null) {
            secret.setDescription(description);
        }
        return secretRepository.save(secret);
    }

    @Transactional
    public void deleteSecret(UUID id) {
        if (!secretRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Secret not found: " + id);
        }
        secretRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Secret getById(UUID id) {
        return secretRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Secret not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Secret> list(Pageable pageable) {
        return secretRepository.findAll(pageable);
    }

    /** Returns decrypted value — admin only, audit this. */
    @Transactional(readOnly = true)
    public String reveal(UUID id) {
        Secret secret = getById(id);
        return vaultEncryptor.decrypt(secret.getEncryptedValue());
    }
}
