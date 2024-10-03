package capaSeguridad;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class TokenEncryptor {

	
	public static String encrypt(String data, String base64Key) throws Exception {
	    // Decodifica la clave desde Base64
	    byte[] decodedKey = Base64.getDecoder().decode(base64Key);
	    
	    // Crea una clave AES desde la clave decodificada
	    SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");
	    
	    // Inicializa el cifrado AES
	    Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
	    
	    // Cifra los datos
	    return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
	}


	public static String decrypt(String encryptedData, String base64Key) throws Exception {
	    // Decodifica la clave desde Base64
	    byte[] decodedKey = Base64.getDecoder().decode(base64Key);
	    
	    // Crea una clave AES desde la clave decodificada
	    SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");
	    
	    // Inicializa el cifrado AES
	    Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.DECRYPT_MODE, secretKey);
	    
	    // Desencripta los datos
	    return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
	}
    
    
    public static  String generateEncryptionKey() {
    	
        String  base64Key ="";
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
	        keyGen.init(256); // Puedes usar 128, 192, o 256 bits
	        SecretKey secretKey = keyGen.generateKey();
	        // Convertir la clave a una cadena base64
	        base64Key = Base64.getEncoder().encodeToString(secretKey.getEncoded());
	        System.out.println("Generated Key: " + base64Key);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	return base64Key;
    }

    
    public static  String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256-bit key
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
    
}
