/**
 * 
 */
/**
 * 
 */
module AESImageEncryptorApp {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics; // Add this line to ensure javafx.graphics is accessible

    exports encryptdec to javafx.graphics;
    opens encryptdec to javafx.fxml;
}