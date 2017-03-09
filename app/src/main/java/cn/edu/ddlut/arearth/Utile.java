package cn.edu.ddlut.arearth;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by WuJie on 2017/3/1.
 */
public class Utile {
    public static AsyncHttpClient client = new AsyncHttpClient();
    public static final String AK = "7PcA50Gp1eGZGM2ZglOaEkKQh435jPEs";
    public static final String SK = "vGxBfa9Oc04BZCqGHbGlxooRKjNgMsG5";
    public static Toast toast;

    public static void t(Context ctx, String str) {
        if(toast == null)
            toast = Toast.makeText(ctx.getApplicationContext(), str, Toast.LENGTH_SHORT);
        toast.setText(str);
        toast.show();
    }

   public static String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest
                    .getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
                        .substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    public static String toQueryString(Map<?, ?> data) throws UnsupportedEncodingException {
        StringBuffer queryString = new StringBuffer();
        for (Map.Entry<?, ?> pair : data.entrySet()) {
            queryString.append(pair.getKey() + "=");
            queryString.append(URLEncoder.encode((String) pair.getValue(),
                    "UTF-8") + "&");
        }
        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }
        return queryString.toString();
    }

    public static String calculateSN(Map<String, String> data) {
        String paramsStr = null;
        try {
            paramsStr = toQueryString(data);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // 对paramsStr前面拼接上/geocoder/v2/?，后面直接拼接yoursk得到/geocoder/v2/?address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourakyoursk
        String wholeStr = new String("/geocoder/v2/?" + paramsStr + SK);
        // 对上面wholeStr再作utf8编码
        String tempStr = null;
        try {
            tempStr = URLEncoder.encode(wholeStr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String r = MD5(tempStr);
        Log.v("===SN===", r);
        return r;
    }
}
