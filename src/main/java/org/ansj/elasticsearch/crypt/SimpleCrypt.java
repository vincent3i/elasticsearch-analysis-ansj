package org.ansj.elasticsearch.crypt;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.elasticsearch.common.lang3.StringUtils;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

/**
 * 简单的DES加解密<BR>
 * 通过这个类可以生成简单的加密字符串作为redis的密码
 * 
 * @author zruan
 *
 */
public class SimpleCrypt {
	
	private static ESLogger logger = Loggers.getLogger("ansj-analyzer");
	
	private static Map<String, String> passStore = new ConcurrentHashMap<String, String>();

	/**
	 * Used to build output as Hex
	 */
	private static final char[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Used to build output as Hex
	 */
	private static final char[] DIGITS_UPPER = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	
	private static final String DES_CRYPT_KEY = "vincent1234567890!@#$%^&*()_+~asdfghjklzxc';/.!@#$";
	
	public static final String DES_ALOGORITHM = "DES";

	/**
	 * 创建密匙
	 * 
	 * @param algorithm
	 *            加密算法,可用 DES,DESede,Blowfish
	 * @return SecretKey 秘密（对称）密钥
	 */
	public static SecretKey getDESKey() {
		SecretKey skey = null;
		
		try {
			//从原始密钥数据创建DESKeySpec对象
			DESKeySpec desKeySpec = new DESKeySpec(DES_CRYPT_KEY.getBytes("UTF-8"));
			
			//创建一个密钥工厂
			SecretKeyFactory factory = SecretKeyFactory.getInstance(DES_ALOGORITHM);
			
			skey = factory.generateSecret(desKeySpec);
			
		} catch (InvalidKeyException e) {
			logger.error("InvalidKeyException", e);
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException", e);
		} catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException", e);
		} catch (InvalidKeySpecException e) {
			logger.error("InvalidKeySpecException", e);
		}
		
		// 返回密匙
		return skey;
	}

	/**
	 * 根据密匙进行DES加密
	 * 
	 * @param key
	 *            密匙
	 * @param info
	 *            要加密的信息
	 * @return String 加密后的信息
	 */
	public static String encryptToDES(SecretKey key, String info) {
		// 定义 加密算法,可用 DES,DESede,Blowfish
		//String algorithm = "DES";
		// 加密随机数生成器 (RNG),(可以不写)
		SecureRandom sr = new SecureRandom();
		// 定义要生成的密文
		byte[] cipherByte = null;
		try {
			// 得到加密/解密器
			Cipher c1 = Cipher.getInstance(DES_ALOGORITHM);
			// 用指定的密钥和模式初始化Cipher对象
			// 参数:(ENCRYPT_MODE, DECRYPT_MODE, WRAP_MODE,UNWRAP_MODE)
			c1.init(Cipher.ENCRYPT_MODE, key, sr);
			// 对要加密的内容进行编码处理,
			cipherByte = c1.doFinal(info.getBytes("UTF-8"));
		} catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException", e);
		} catch (NoSuchPaddingException e) {
			logger.error("NoSuchPaddingException", e);
		} catch (InvalidKeyException e) {
			logger.error("InvalidKeyException", e);
		} catch (IllegalBlockSizeException e) {
			logger.error("IllegalBlockSizeException", e);
		} catch (BadPaddingException e) {
			logger.error("BadPaddingException", e);
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException", e);
		}
		// 返回密文的十六进制形式
		return new String(encodeHex(cipherByte));
	}

	/**
	 * 根据密匙进行DES解密
	 * 
	 * @param key
	 *            密匙
	 * @param sInfo
	 *            要解密的密文
	 * @return String 返回解密后信息
	 */
	public static String decryptByDES(SecretKey key, String sInfo) {
		// 定义 加密算法,
		//String Algorithm = "DES";
		// 加密随机数生成器 (RNG)
		SecureRandom sr = new SecureRandom();
		byte[] cipherByte = null;
		try {
			// 得到加密/解密器
			Cipher c1 = Cipher.getInstance(DES_ALOGORITHM);
			// 用指定的密钥和模式初始化Cipher对象
			c1.init(Cipher.DECRYPT_MODE, key, sr);
			// 对要解密的内容进行编码处理
			cipherByte = c1.doFinal(decodeHex(sInfo.toCharArray()));
		} catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException", e);
		} catch (NoSuchPaddingException e) {
			logger.error("NoSuchPaddingException", e);
		} catch (InvalidKeyException e) {
			logger.error("InvalidKeyException", e);
		} catch (IllegalBlockSizeException e) {
			logger.error("IllegalBlockSizeException", e);
		} catch (BadPaddingException e) {
			logger.error("BadPaddingException", e);
		} catch (DecoderException e) {
			logger.error("DecoderException", e);
		}

		return new String(cipherByte);
	}

	/**
	 * Converts an array of characters representing hexadecimal values into an
	 * array of bytes of those same values. The returned array will be half the
	 * length of the passed array, as it takes two characters to represent any
	 * given byte. An exception is thrown if the passed char array has an odd
	 * number of elements.
	 * 
	 * @param data
	 *            An array of characters containing hexadecimal digits
	 * @return A byte array containing binary data decoded from the supplied
	 *         char array.
	 * @throws DecoderException
	 *             Thrown if an odd number or illegal of characters is supplied
	 */
	public static byte[] decodeHex(char[] data) throws DecoderException {

		int len = data.length;

		if ((len & 0x01) != 0) {
			throw new DecoderException("Odd number of characters.");
		}

		byte[] out = new byte[len >> 1];

		// two characters form the hex value.
		for (int i = 0, j = 0; j < len; i++) {
			int f = toDigit(data[j], j) << 4;
			j++;
			f = f | toDigit(data[j], j);
			j++;
			out[i] = (byte) (f & 0xFF);
		}

		return out;
	}

	/**
	 * Converts a hexadecimal character to an integer.
	 * 
	 * @param ch
	 *            A character to convert to an integer digit
	 * @param index
	 *            The index of the character in the source
	 * @return An integer
	 * @throws DecoderException
	 *             Thrown if ch is an illegal hex character
	 */
	protected static int toDigit(char ch, int index) throws DecoderException {
		int digit = Character.digit(ch, 16);
		if (digit == -1) {
			throw new DecoderException("Illegal hexadecimal character " + ch
					+ " at index " + index);
		}
		return digit;
	}

	/**
	 * Converts an array of bytes into an array of characters representing the
	 * hexadecimal values of each byte in order. The returned array will be
	 * double the length of the passed array, as it takes two characters to
	 * represent any given byte.
	 * 
	 * @param data
	 *            a byte[] to convert to Hex characters
	 * @return A char[] containing hexadecimal characters
	 */
	public static char[] encodeHex(byte[] data) {
		return encodeHex(data, true);
	}

	/**
	 * Converts an array of bytes into an array of characters representing the
	 * hexadecimal values of each byte in order. The returned array will be
	 * double the length of the passed array, as it takes two characters to
	 * represent any given byte.
	 * 
	 * @param data
	 *            a byte[] to convert to Hex characters
	 * @param toLowerCase
	 *            <code>true</code> converts to lowercase, <code>false</code> to
	 *            uppercase
	 * @return A char[] containing hexadecimal characters
	 * @since 1.4
	 */
	public static char[] encodeHex(byte[] data, boolean toLowerCase) {
		return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
	}

	/**
	 * Converts an array of bytes into an array of characters representing the
	 * hexadecimal values of each byte in order. The returned array will be
	 * double the length of the passed array, as it takes two characters to
	 * represent any given byte.
	 * 
	 * @param data
	 *            a byte[] to convert to Hex characters
	 * @param toDigits
	 *            the output alphabet
	 * @return A char[] containing hexadecimal characters
	 * @since 1.4
	 */
	protected static char[] encodeHex(byte[] data, char[] toDigits) {
		int l = data.length;
		char[] out = new char[l << 1];
		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
			out[j++] = toDigits[0x0F & data[i]];
		}
		return out;
	}
	
	public String encrypt(String decoded) {
		if(null == decoded || "".equals(decoded)) {
			logger.info("Decoded value is empty");
			return "";
		}
		
		String encoded = encryptToDES(getDESKey(), decoded);
		return null == encoded ? StringUtils.EMPTY : encoded;
	}
	
	public String decrypt(String encoded) {
		if(null == encoded || "".equals(encoded)) {
			logger.info("Encoded value is empty");
			return "";
		}
		
		//加密后解密使用map方式提升性能
		String decoded = passStore.get(encoded);
		if(null == decoded) {
			decoded = decryptByDES(getDESKey(), encoded);
			if(StringUtils.isEmpty(decoded)) {
				return StringUtils.EMPTY;
			}
			
			passStore.put(decoded, encoded);
		}
		
		return decoded;
	}

	/**
	 * remove it when used in a formal env
	 * @param args
	 */
	public static void main(String[] args) {
		if(null == args || args.length < 2) {
			throw new IllegalArgumentException("Arguments not matched! style --->> encode|decode password");
		}
		
		String type = args[0].toLowerCase();
		String pass = args[1];
		
		if("decode".equals(type)) {
			//please forbiden this function when your have any secure request
			String decryptPass = decryptByDES(getDESKey(), pass);
			System.out.println("Decode password [" + pass + "] as below --->>> ");
			System.out.println("*******************************************************************");
			System.out.println(decryptPass);
			System.out.println("*******************************************************************");
		} else if ("encode".equals(type)) {
			String encryptPass = encryptToDES(getDESKey(), pass);
			System.out.println("Encode password [" + pass + "] as below --->>> ");
			System.out.println("*******************************************************************");
			System.out.println(encryptPass);
			System.out.println("*******************************************************************");
		}
	}
}
