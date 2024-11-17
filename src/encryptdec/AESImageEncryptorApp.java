package encryptdec;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Base64;

public class AESImageEncryptorApp extends Application {

    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private TextArea outputArea;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Image Encryption/Decryption");

        // Main layout
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        // Buttons for Encrypt and Decrypt
        Button encryptButton = new Button("Encrypt");
        Button decryptButton = new Button("Decrypt");
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(200);

        // Event handling for Encrypt
        encryptButton.setOnAction(e -> showEncryptDialog(primaryStage));

        // Event handling for Decrypt
        decryptButton.setOnAction(e -> showDecryptDialog(primaryStage));

        // Adding buttons to the layout
        grid.add(encryptButton, 0, 0);
        grid.add(decryptButton, 1, 0);
        grid.add(outputArea, 0, 1, 2, 1);

        // Setting up the stage
        Scene scene = new Scene(grid, 500, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Show encryption dialog
    private void showEncryptDialog(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Input Folder");

        File inputDir = directoryChooser.showDialog(primaryStage);
        if (inputDir == null) return;

        directoryChooser.setTitle("Select Output Folder");
        File outputDir = directoryChooser.showDialog(primaryStage);
        if (outputDir == null) return;

        try {
            SecretKey key = generateKey();
            processDirectory(inputDir.getAbsolutePath(), outputDir.getAbsolutePath(), key, true);

            String keyString = Base64.getEncoder().encodeToString(key.getEncoded());
            outputArea.setText("Encryption completed.\nSecret Key (Save this for decryption): " + keyString);
        } catch (Exception ex) {
            outputArea.setText("Error during encryption: " + ex.getMessage());
        }
    }

    // Show decryption dialog
    private void showDecryptDialog(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Encrypted Folder");

        File inputDir = directoryChooser.showDialog(primaryStage);
        if (inputDir == null) return;

        directoryChooser.setTitle("Select Output Folder");
        File outputDir = directoryChooser.showDialog(primaryStage);
        if (outputDir == null) return;

        TextInputDialog keyDialog = new TextInputDialog();
        keyDialog.setTitle("Decryption Key");
        keyDialog.setHeaderText("Enter the key for decryption:");
        keyDialog.setContentText("Key:");

        String keyString = keyDialog.showAndWait().orElse("");
        if (keyString.isEmpty()) {
            outputArea.setText("Decryption key not provided.");
            return;
        }

        try {
            SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(keyString), "AES");
            processDirectory(inputDir.getAbsolutePath(), outputDir.getAbsolutePath(), key, false);
            outputArea.setText("Decryption completed. Files saved in: " + outputDir.getAbsolutePath());
        } catch (Exception ex) {
            outputArea.setText("Error during decryption: " + ex.getMessage());
        }
    }

    // Process the directory for encryption/decryption
    private static void processDirectory(String inputDir, String outputDir, SecretKey key, boolean isEncrypt) throws Exception {
        File inputDirectory = new File(inputDir);
        File outputDirectory = new File(outputDir);

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        for (File file : inputDirectory.listFiles()) {
            if (file.isFile() && file.getName().toLowerCase().matches(".*\\.(png|jpg|jpeg|bmp|enc)")) {
                File outputFile = new File(outputDirectory, isEncrypt ? file.getName() + ".enc" : file.getName().replace(".enc", ""));
                if (isEncrypt) {
                    encryptFile(file, outputFile, key);
                } else {
                    decryptFile(file, outputFile, key);
                }
            }
        }
    }

    // Encrypt a file
    private static void encryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] fileData = Files.readAllBytes(inputFile.toPath());
        byte[] encryptedData = cipher.doFinal(fileData);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(iv);
            fos.write(encryptedData);
        }
    }

    // Decrypt a file
    private static void decryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        byte[] fileData = Files.readAllBytes(inputFile.toPath());
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(fileData, 0, iv, 0, GCM_IV_LENGTH);

        byte[] encryptedData = new byte[fileData.length - GCM_IV_LENGTH];
        System.arraycopy(fileData, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        byte[] decryptedData = cipher.doFinal(encryptedData);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(decryptedData);
        }
    }

    // Generate a new AES key
    private static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE);
        return keyGenerator.generateKey();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
