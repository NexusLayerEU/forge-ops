package eu.forgeops.engine.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import eu.forgeops.domain.inventory.Node;
import eu.forgeops.domain.vault.Secret;
import eu.forgeops.domain.vault.VaultEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class SshConnectionManager {

    private static final int CONNECT_TIMEOUT_MS = 15_000;

    private final VaultEncryptor vaultEncryptor;

    public Session openSession(Node node, Map<String, Object> resolvedVars) throws Exception {
        JSch jsch = new JSch();
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "publickey,password");
        config.put("ServerAliveInterval", "30");
        config.put("ServerAliveCountMax", "3");

        String host = node.getHostname();
        int port = node.getPort();

        // SSH user from variables, fall back to root
        String user = resolvedVars != null
            ? (String) resolvedVars.getOrDefault("ssh_user",
                resolvedVars.getOrDefault("ansible_user", "root"))
            : "root";

        Session session = jsch.getSession(user, host, port);
        session.setConfig(config);
        session.setTimeout(CONNECT_TIMEOUT_MS);

        Secret cred = node.getCredential();
        if (cred != null) {
            String decrypted = vaultEncryptor.decrypt(cred.getEncryptedValue());
            if (cred.getSecretType() == Secret.SecretType.ssh_key) {
                byte[] keyBytes = decrypted.getBytes();
                jsch.addIdentity("forgeops-key-" + node.getId(), keyBytes, null, null);
            } else {
                session.setPassword(decrypted);
            }
        }

        session.connect(CONNECT_TIMEOUT_MS);
        log.debug("SSH session opened to {}:{} as {}", host, port, user);
        return session;
    }

    public void closeSession(Session session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
