/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import com.apollocurrency.aplwallet.apl.core.app.EncryptedSecretBytesDetails;
import com.apollocurrency.aplwallet.apl.core.app.SecretBytesDetails;
import com.apollocurrency.aplwallet.apl.core.app.VaultKeyStore;
import com.apollocurrency.aplwallet.apl.core.app.VaultKeyStoreImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@EnableWeld
public class VaultKeyStoreTest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            NtpTime.class
    ).build();

    private static final String PASSPHRASE = "random passphrase generated by passphrase generator";
    private static final String ACCOUNT1 = "APL-299N-Y6F7-TZ8A-GYAB8";
    private static final String ACCOUNT2 = "APL-Z6D2-YTAB-L6BV-AAEAY";
    private static final String encryptedKeyJSON =
            "{\n" +
                    "  \"encryptedSecretBytes\" : \"8qWMzLfNJt4wT0q2n7YuyMouj08hbfzx9z9HuIBZf2tGHqajPXfHpwzV6EwKYTWMDa2j3copDxujx2SLmFXwdA==\",\n" +
                    "  \"accountRS\" : \"APL-299N-Y6F7-TZ8A-GYAB8\",\n" +
                    "  \"account\" : -2079221632084206348,\n" +
                    "  \"version\" : 0,\n" +
                    "  \"nonce\" : \"PET2LeUQDMfgrCIvM0j0tA==\",\n" +
                    "  \"timestamp\" : 1539036932840\n" +
                    "}";
    private static final String SECRET_BYTES_1 = "44a2868161a651682bdf938b16c485f359443a2c53bd3e752046edef20d11567";
    private static final String SECRET_BYTES_2 = "146c55cbdc5f33390d207d6d08030c3dd4012c3f775ed700937a893786393dbf";
    private byte[] secretBytes = generateSecretBytes();
    private byte[] nonce = new byte[16];

    private byte[] generateSecretBytes() {
        byte secretBytes[] = new byte[32];
        Random random = new Random();
        random.nextBytes(secretBytes);
        return secretBytes;
    }

    private Path tempDirectory;
    private VaultKeyStoreImpl keyStore;

    @BeforeEach
    void setUp() throws Exception {
//        Crypto.getSecureRandom().nextBytes(nonce);
        tempDirectory = Files.createTempDirectory("keystore-test");
        keyStore = new VaultKeyStoreImpl(tempDirectory, (byte) 0);
        Files.write(tempDirectory.resolve("---" + ACCOUNT1), encryptedKeyJSON.getBytes());
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.list(tempDirectory).forEach(tempFilePath -> {
            try {
                Files.delete(tempFilePath);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Files.delete(tempDirectory);
    }

    @Test
    public void testSaveKey() throws Exception {
        VaultKeyStoreImpl keyStoreSpy = spy(keyStore);

        VaultKeyStore.Status status = keyStoreSpy.saveSecretBytes(PASSPHRASE, Convert.parseHexString(SECRET_BYTES_2));
        assertEquals(VaultKeyStore.Status.OK, status);
        verify(keyStoreSpy, times(1)).storeJSONSecretBytes(any(Path.class), any(EncryptedSecretBytesDetails.class));
        verify(keyStoreSpy, times(1)).findSecretPaths(anyLong());

        assertEquals(2, Files.list(tempDirectory).count());

        String rsAcc = Convert.defaultRsAccount(Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(Convert.parseHexString(SECRET_BYTES_2)))));

        Path encryptedKeyPath =
                Files.list(tempDirectory).filter(path -> path.getFileName().toString().endsWith(rsAcc)).findFirst().orElseThrow(()->new RuntimeException("No encrypted key found for " + rsAcc  + " account"));

        EncryptedSecretBytesDetails KeyDetails = JSON.getMapper().readValue(encryptedKeyPath.toFile(), EncryptedSecretBytesDetails.class);

        byte[] actualKey = Crypto.aesDecrypt(KeyDetails.getEncryptedSecretBytes(), Crypto.getKeySeed(PASSPHRASE,
                KeyDetails.getNonce(), Convert.longToBytes(KeyDetails.getTimestamp())));

        assertEquals(SECRET_BYTES_2, Convert.toHexString(actualKey));
        assertEquals(ACCOUNT2, rsAcc);
    }


    @Test
    public void testGetKey() throws Exception {


        VaultKeyStoreImpl keyStoreSpy = spy(keyStore);

        long accountId = Convert.parseAccountId(ACCOUNT1);
        SecretBytesDetails secretBytes = keyStoreSpy.getSecretBytesV0(PASSPHRASE, accountId);
        byte[] actualKey = secretBytes.getSecretBytes();
        assertEquals(VaultKeyStore.Status.OK, secretBytes.getExtractStatus()) ;
        String rsAcc = Convert.defaultRsAccount(accountId);

        verify(keyStoreSpy, times(1)).findSecretPaths(accountId);

        assertEquals(1, Files.list(tempDirectory).count());
        Path encryptedKeyPath = Files.list(tempDirectory).findFirst().get();
        assertTrue(encryptedKeyPath.getFileName().toString().endsWith(rsAcc));

        assertEquals(SECRET_BYTES_1, Convert.toHexString(actualKey));

    }
    @Test
    public void testGetKeyUsingIncorrectPassphrase() {
        long accountId = Convert.parseAccountId(ACCOUNT1);
        SecretBytesDetails secretBytesDetails = keyStore.getSecretBytesV0("pass", accountId);
        assertNull(secretBytesDetails.getSecretBytes());
        assertEquals(VaultKeyStore.Status.DECRYPTION_ERROR, secretBytesDetails.getExtractStatus());
    }

    @Test
    public void testGetKeyUsingIncorrectAccount() throws Exception {
        long accountId = 0;
        SecretBytesDetails secretBytesDetails = keyStore.getSecretBytesV0(PASSPHRASE, accountId);
        assertNull(secretBytesDetails.getSecretBytes());
        assertEquals(VaultKeyStore.Status.NOT_FOUND, secretBytesDetails.getExtractStatus());
    }

    @Test
    public void testSaveDuplicateKey() throws IOException {
        VaultKeyStore.Status status = keyStore.saveSecretBytes(PASSPHRASE, Convert.parseHexString(SECRET_BYTES_1));
        assertEquals(VaultKeyStore.Status.DUPLICATE_FOUND, status);
    }

    @Test
    public void testDeleteKey() {
        VaultKeyStore.Status status = keyStore.deleteSecretBytes(PASSPHRASE, Convert.parseAccountId(ACCOUNT1));
        assertEquals(VaultKeyStore.Status.OK, status);
    }

    @Test
    public void testDeleteNotFound() {
        VaultKeyStore.Status status = keyStore.deleteSecretBytes(PASSPHRASE, Convert.parseAccountId(ACCOUNT2));
        assertEquals(VaultKeyStore.Status.NOT_FOUND, status);
    }

    @Test
    public void testDeleteIncorrectPassphrase() {
        VaultKeyStore.Status status = keyStore.deleteSecretBytes(PASSPHRASE + "0", Convert.parseAccountId(ACCOUNT1));
        assertEquals(VaultKeyStore.Status.DECRYPTION_ERROR, status);
    }

    @Test
    public void testDeleteIOError() throws IOException {
        VaultKeyStoreImpl spiedKeyStore = Mockito.spy(keyStore);
        doThrow(new IOException()).when(spiedKeyStore).deleteFile(any(Path.class));
        VaultKeyStore.Status status = spiedKeyStore.deleteSecretBytes(PASSPHRASE, Convert.parseAccountId(ACCOUNT1));
        assertEquals(VaultKeyStore.Status.DELETE_ERROR, status);
        verify(spiedKeyStore, times(1)).deleteFile(any(Path.class));
    }

    @Test
    public void testDeleteNotAvailable() throws IOException {
        Path path = tempDirectory.resolve(".local");
        try {
            Files.createFile(path);
            VaultKeyStore.Status status = keyStore.deleteSecretBytes(PASSPHRASE, Convert.parseAccountId(ACCOUNT1));
            assertEquals(VaultKeyStore.Status.NOT_AVAILABLE, status);

        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void testSaveNotAvailable() throws IOException {
        Path path = tempDirectory.resolve(".local");
        try {
            Files.createFile(path);
            VaultKeyStore.Status status = keyStore.saveSecretBytes(PASSPHRASE, Convert.parseHexString(SECRET_BYTES_2));
            assertEquals(VaultKeyStore.Status.NOT_AVAILABLE, status);

        } finally {
            Files.deleteIfExists(path);
        }
    }
    @Test
    public void testGetNotAvailable() throws IOException {
        Path path = tempDirectory.resolve(".local");
        try {
            Files.createFile(path);
            SecretBytesDetails secretBytes = keyStore.getSecretBytesV0(PASSPHRASE, Convert.parseAccountId(ACCOUNT1));
            assertEquals(VaultKeyStore.Status.OK, secretBytes.getExtractStatus());
            assertEquals(SECRET_BYTES_1, Convert.toHexString(secretBytes.getSecretBytes()));

        } finally {
            Files.deleteIfExists(path);
        }
    }
}

