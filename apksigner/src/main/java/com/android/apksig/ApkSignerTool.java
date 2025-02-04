package com.android.apksig;

import com.google.common.collect.ImmutableList;
import java.io.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import com.android.apksig.apk.ApkFormatException;

public class ApkSignerTool {

  private static File mInputFile;
  private static File mOutputFile;  
  private static String SIGNER_NAME;

  private static File mStoreFile;
  private static String mKeyAlias;
  private static String mStorePassword;
  private static String mKeyPassword;

  private static File mTestKeyFile;
  private static File mTestCertFile;

  public ApkSignerTool() {}

  public static void main(String[] args) throws IOException, ApkFormatException, Exception {
	 	
    //InputStream inputStream = new FileInputStream(getTestCertFile());
	//KeyStore keyStore = getKeyStore(inputStream, getKeyPassword());			
    //ApkSigner.SignerConfig signerConfig = new ApkSigner.SignerConfig.Builder(getSignerConfig(keyStore,getKeyAlias(),getStorePassword()),false).build();
    
	ApkSigner apkSigner = new ApkSigner.Builder(ImmutableList.of(getDebugSignerConfig())).setInputApk(getInputFile())
    		.setOutputApk(getOutPutFile()).build();
    apkSigner.sign();

  }
  
  public static void setInputFile(File input) {
    mInputFile = input;
  }

  public static void setOutPutFile(File output) {
    mOutputFile = output;
  }
  
  public static File getInputFile() {
    if (mInputFile != null) {
      return new File( mInputFile.getAbsolutePath());
    }
    return null;
  }

  public static File getOutPutFile() {
    if (mOutputFile != null) {
      return new File(mOutputFile.getAbsolutePath());
    }
    return null;
  }

  public static void setStoreFile(File store_file) {
    mStoreFile = store_file;
  }

  public static void setKeyAlias(String key_alias) {
    mKeyAlias = key_alias;
  }

  public static void setStorePassword(String password) {
    mStorePassword = password;
  }

  public static void setKeyPassword(String keyPassword) {
    mKeyPassword = keyPassword;
  }

  public static String getStoreFile() {
    if (mStoreFile != null) {
      return mStoreFile.getAbsolutePath();
    }
    return null;
  }

  public static String getKeyAlias() {
    if (mKeyAlias != null) {
      return mKeyAlias;
    }
    return null;
  }

  public static String getStorePassword() {
    if (mStorePassword != null) {
      return mStorePassword;
    }
    return null;
  }

  public static String getKeyPassword() {
    if (mKeyPassword != null) {
      return mKeyPassword;
    }
    return null;
  }

  public static void setTestKeyFile(File file) {
    mTestKeyFile = file;
  }

  public static void setTestCertFile(File file) {
    mTestCertFile = file;
  }

  public static String getTestKeyFile() {
    if (mTestKeyFile != null) {
      return mTestKeyFile.getAbsolutePath();
    }
    return null;
  }

  public static String getTestCertFile() {
    if (mTestCertFile != null) {
      return mTestCertFile.getAbsolutePath();
    }
    return null;
  }

  public static PrivateKey loadPkcs8EncodedPrivateKey(PKCS8EncodedKeySpec spec)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    try {
      return KeyFactory.getInstance("RSA").generatePrivate(spec);
    } catch (InvalidKeySpecException e) {
      // ignore
    }
    try {
      return KeyFactory.getInstance("EC").generatePrivate(spec);
    } catch (InvalidKeySpecException e) {
      // ignore
    }
    try {
      return KeyFactory.getInstance("DSA").generatePrivate(spec);
    } catch (InvalidKeySpecException e) {
      // ignore
    }
    throw new InvalidKeySpecException("Not an RSA, EC, or DSA private key");
  }

  public static ApkSigner.SignerConfig getDebugSignerConfig() throws Exception {

    byte[] privateKeyBlob = Files.readAllBytes(Paths.get(getTestKeyFile()));
    InputStream pemInputStream = new FileInputStream(getTestCertFile());

    PrivateKey privateKey;
    try {
      final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBlob);
      privateKey = loadPkcs8EncodedPrivateKey(keySpec);
    } catch (InvalidKeySpecException e) {
      throw new InvalidKeySpecException("Failed to load PKCS #8 encoded private key ", e);
    }

    final List<Certificate> certs =
        ImmutableList.copyOf(
            CertificateFactory.getInstance("X.509").generateCertificates(pemInputStream).stream()
                .map(c -> (Certificate) c)
                .collect(Collectors.toList()));

    X509Certificate cert = (X509Certificate) certs.get(0);
    initSignerName(cert);

    final List<X509Certificate> x509Certs =
        Collections.checkedList(
            certs.stream().map(c -> (X509Certificate) c).collect(Collectors.toList()),
            X509Certificate.class);

    pemInputStream.close();

    return new ApkSigner.SignerConfig.Builder(SIGNER_NAME, privateKey, x509Certs).build();
  }

  public static byte[] readAllBytes(InputStream inputStream) throws IOException {
    final byte[] buffer = new byte[8192];
    int bytesRead;
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    return outputStream.toByteArray();
  }

  public static void initSignerName(X509Certificate cert) {
    final String defaultName = "CERT";
    try {
      final Properties properties = new Properties();
      final String subjectName = cert.getSubjectX500Principal().getName().replace(',', '\n');
      properties.load(new StringReader(subjectName));
      SIGNER_NAME = properties.getProperty("CN", defaultName);
    } catch (Exception e) {
      SIGNER_NAME = defaultName;
    }
  }

  public static KeyStore getKeyStore(InputStream inputStream, String password) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputStream.available());
    byte[] buffer = new byte[4096];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    ByteArrayInputStream data = new ByteArrayInputStream(outputStream.toByteArray());
    KeyStore keystore = isJKS(data) ? new JavaKeyStore() : KeyStore.getInstance("PKCS12");
    keystore.load(data, password.toCharArray());
    return keystore;
  }

  public static boolean isJKS(InputStream data) {
    try (final DataInputStream dis = new DataInputStream(new BufferedInputStream(data))) {
      return dis.readInt() == 0xfeedfeed;
    } catch (Exception e) {
      return false;
    }
  }

  public static ApkSigner.SignerConfig getSignerConfig(
      KeyStore keystore, String keyAlias, String aliasPassword) throws Exception {
    PrivateKey privateKey =
        (PrivateKey)
            keystore.getKey(
                keyAlias,
                new KeyStore.PasswordProtection(aliasPassword.toCharArray()).getPassword());
    if (privateKey == null) {
      throw new RuntimeException("No key found with alias '" + keyAlias + "' in keystore.");
    }
    X509Certificate[] certChain = (X509Certificate[]) keystore.getCertificateChain(keyAlias);
    if (certChain == null) {
      throw new RuntimeException(
          "No certificate chain found with alias '" + keyAlias + "' in keystore.");
    }
    ImmutableList<X509Certificate> certificates =
        Arrays.stream(certChain)
            .map(
                cert -> {
                  initSignerName(cert);
                  return cert;
                })
            .collect(ImmutableList.toImmutableList());
    return new ApkSigner.SignerConfig.Builder(SIGNER_NAME, privateKey, certificates).build();
  }
}
