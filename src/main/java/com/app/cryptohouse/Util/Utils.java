package com.app.cryptohouse.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;

public class Utils {
	
	public static String requestAPI(String url) {
		String result = null;
		try {
			URL reqUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) reqUrl.openConnection();
			conn.setRequestMethod("GET");
			result = convertInputStreamToString(conn.getInputStream());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return result;
	}

    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
        String line = "", result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
    }
    
    /**
     * 숫자에 천단위마다 콤마 넣기
     * @param int
     * @return String
     * */
    public static String toNumFormat(Long num) {
        DecimalFormat df = new DecimalFormat("###,###,###.####");
        return df.format(num);
    }

    /**
     * 콤마 제거
     */
    public static String delNumFormat(String str){
        return str.replace(",","");
    }
}
