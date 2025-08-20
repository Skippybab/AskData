package com.mt.agent.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密工具
 *
 */
public class MD5Util {

	/**
	 * MD5加密
	 *
	 * @param plainText
	 *            原密文
	 * @return 加密后的密文
	 */
	public static String MD5Purity(String plainText) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(plainText.getBytes("UTF-8"));
			byte b[] = md.digest();
			int i;
			StringBuffer buf = new StringBuffer("");
			for (int offset = 0; offset < b.length; offset++) {
				i = b[offset];
				if (i < 0) {
					i += 256;
				}
				if (i < 16) {
					buf.append("0");
				}
				buf.append(Integer.toHexString(i));
			}
			plainText = buf.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return plainText;
	}

	/**
	 * MD5加密验证
	 *
	 * @param plainText
	 *            原密文
	 * @param password
	 *            需验证的密文
	 * @return 验证结果
	 */
	public static boolean MD5PurityVerification(String plainText, String password) {
		plainText = MD5Purity(plainText);
		if (password.equals(plainText)) {
			return true;
		}else {
			return false;
		}
	}

	/**
	 * 原密文加密后分解为两段，分别加密后合并为一个长字符串，再次进行加密
	 *
	 * @param plainText
	 *            原密文
	 * @return 加密后的密文
	 */
	public static String MD5X2(String plainText) {
		// 先把密码加密成长度为32字符的密文
		String s = new StringBuffer(MD5Purity(plainText)).toString();
		// 把密码分割成两段
		String left = s.substring(0, 16);
		String right = s.substring(16, 32);
		// 分别加密后再合并
		s = MD5Purity(left) + MD5Purity(right);
		// 最后把长字串再加密一次，成为32字符密文
		return MD5Purity(s);
	}

	/**
	 * 两段加密验证
	 *
	 * @param plainText
	 *            原密文
	 * @param password
	 *            需验证的密文
	 * @return 验证结果
	 */
	public static boolean MD5X2Verification(String plainText, String password) {
		plainText = MD5X2(plainText);
		if (password.equals(plainText)) {
			return true;
		}else {
			return false;
		}
	}

	public static void main(String[] args) {
		//31dd9cba757467657e94916fcbd91fef
		System.out.println(MD5X2("123456"));
	}

}
