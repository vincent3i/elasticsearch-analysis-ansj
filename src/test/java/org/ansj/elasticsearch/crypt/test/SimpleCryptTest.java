package org.ansj.elasticsearch.crypt.test;

import static org.junit.Assert.*;
import static org.ansj.elasticsearch.crypt.SimpleCrypt.*;

import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Test;

public class SimpleCryptTest {
	
	private SecretKey secretKey;
	
	@Before
	public void before() {
		secretKey = getDESKey();
	}

	@Test
	public void cryptTest() {
		String[] passes = {"Welcome1234!@#$", "pass!@#$", "pwd!"};
		for (String pass : passes) {
			String encryptPass = encryptToDES(secretKey, pass);
			System.out.println("Encode password [" + pass + "] as below --->>> ");
			System.out.println("*******************************************************************");
			System.out.println(encryptPass);
			System.out.println("*******************************************************************");
			
			String decryptPass = decryptByDES(getDESKey(), encryptPass);
			System.out.println("Decode password [" + decryptPass + "] as below --->>> ");
			System.out.println("*******************************************************************");
			System.out.println(decryptPass);
			System.out.println("*******************************************************************");
			
			assertEquals(pass, decryptPass);
			
			encryptPass = encrypt(pass);
			decryptPass = decrypt(encryptPass);
			assertEquals(pass, decryptPass);
		}
	}
}
