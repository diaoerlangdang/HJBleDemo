package com.wise.ble;


import java.io.UnsupportedEncodingException;

public class ConvertData
{

	public static String bytesToUtf8(byte[] bytes) {

		try {
			return new String(bytes, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static byte[] utf8ToBytes(String utf8Str) {

		try {
			return utf8Str.getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

    /**
     * 将byte数组转换为16进制格式的字符串
     *
     * @param bytes byte数组
     * @param bSpace  是否在每两个数组中间添加空格
     *
     * @return 返回16进制格式的字符串
     */
	public static String bytesToHexString(byte[] bytes, boolean bSpace)
	{
		if(bytes == null || bytes.length <= 0)
			return null;
		
		StringBuffer stringBuffer = new StringBuffer(bytes.length);
		String sTemp;
		
		for (int i = 0; i < bytes.length; i++) 
		{
			sTemp = Integer.toHexString(0xFF & bytes[i]);
			
			if (sTemp.length() < 2)
				stringBuffer.append(0);
			
			stringBuffer.append(sTemp);
			
			if(bSpace)
				stringBuffer.append(" ");
		}
		return stringBuffer.toString();
	}
	
    /**
     * 将字符串转换为byte数组
     *
     * @param hexString 16进制格式的字符串（仅包含0-9，a-f,A-F,且长度为偶数)
     *
     * @return 返回转换后的byte数组
     */
	public static byte[] hexStringToBytes(String hexString)
	{
		if(hexString == null)
			return null;
		
		hexString = hexString.replace(" ", "");
		hexString = hexString.toUpperCase();
		
		int len = (hexString.length() / 2);
		if(len <= 0)
			return null;
		
		byte[] result = new byte[len];
		char[] achar = hexString.toCharArray();
		for (int i = 0; i < len; i++) 
		{
			int pos = i * 2;
			result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
		}
		
		return result;
	}
	
    /**
     * 将一个数组拷贝到另一个数组上
     *
     * @param dst 目标数组
     * @param dstOffset 目标数组偏移
     * @param src 源数组
     * @param srcOffset 目标数组偏移
     * @param length 拷贝的长度
     *
     * @return 成功返回true，否则false
     */
	public static boolean cpyBytes(byte[] dst, int dstOffset, byte[] src, int srcOffset, int length)
	{
		if(dst == null || src == null || 
				dstOffset > dst.length || srcOffset > src.length ||
				length > (dst.length-dstOffset) || length > (src.length-srcOffset))
		{
			return false;
		}
		
		for (int i = 0; i < length; i++)
		{
			dst[i+dstOffset] = src[i+srcOffset];
		}
		
		return true;
	}
	
    /**
     * 两个数组比较
     *
     * @param data1 数组1
     * @param data2 数组2
     *
     * @return 相等返回true，否则返回false
     */
	public static boolean cmpBytes(byte[] data1, byte[] data2)
	{
		if (data1 == null && data2 == null)
		{
			return true;
		}
		if (data1 == null || data2 == null) 
		{
			return false;
		}
		if (data1 == data2)
		{
			return true;
		}
		if(data1.length != data2.length)
		{
			return false;
		}
		
		int len = data1.length;
		for (int i = 0; i < len; i++)
		{
			if(data1[i] != data2[i])
				return false;
		}
		
		return true;
	}

	/**
	 * 校验和
	 * @param data		数据
	 * @param offset	偏移
	 * @param length	长度
	 * @return 校验和
	 */
	public static byte checkSumData(byte[] data, int offset, int length)
	{
		if (offset >= length || length > data.length) {
			return 0;
		}

		byte sum = 0;
		for (int i=offset; i<offset+length; i++) {
			sum = (byte) ((data[i] + sum) & 0xFF);
		}

		return sum;
	}

	/**
	 * 翻转数据
	 * @param data	数据
	 * @return 翻转后的数据
	 */
	public static byte[] reversalData(byte[] data)
	{
		if (data == null) {
			return null;
		}

		byte[] resultData = new byte[data.length];

		for (int i=0; i<resultData.length; i++) {
			resultData[i] = data[data.length-1-i];
		}

		return resultData;
	}


	
	private static int toByte(char c) 
	{
	    byte b = (byte) "0123456789ABCDEF".indexOf(c);
	    return b;
	}

}
