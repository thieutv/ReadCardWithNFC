package cmc.com.readidcardwithnfc.util;

import android.util.Log;

import org.bouncycastle.util.encoders.Hex;

public class StringUtil {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToByte(String hex) {
        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(hex.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

    public static String hexToASCII(String hex) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    public static String hexToUTF8(String hex) throws Exception {
        byte[] arr = hexToByte(hex);
        String a = new String(arr, "UTF-8");
        return a;
    }

    //Phương thức để lấy chuỗi hex biểu diễn không tin độ dài của trường value trong
    //cấu trúc dữ liệu TLV
    public static String cutTLVString(String hex, String tag) {
        String countString = "";
        int index = 0;
        if (tag == null) {
            Log.d(TAG, "Tag is not null!");
            return null;
        }

        index = hex.indexOf(tag) + tag.length();
        countString = hex.substring(index, index + 2);
        return countString;
    }

    //Phương thức để lấy vị trí của chuỗi mà nới đó xuất hiện chuỗi tag + chỗi length
    //việc này để lấy vị trí cho việc cắt chuỗi đúng vị trí.
    public static int indexOfTagWithLength(String hex, String tag) {
        int index = 0;
        if (tag == null) {
            Log.d(TAG, "Tag length not null!");
            return -1;
        }
        index = hex.indexOf(tag) + tag.length() + 2;
        return index;
    }

    private static final String TAG = StringUtil.class.getName();
}
