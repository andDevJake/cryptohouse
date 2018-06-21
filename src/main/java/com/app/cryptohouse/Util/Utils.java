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
		String clientId     = "oHvZMvSricYeIt0b9hKN";//애플리케이션 클라이언트 아이디값
		String clientSecret = "thPUDjcvP0";//애플리케이션 클라이언트 시크릿값
		
		String result = null;
		try {
			URL reqUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) reqUrl.openConnection();
			conn.setRequestMethod("GET");
			
			if(url.contains("naver")) {
				conn.setRequestProperty("X-Naver-Client-Id", clientId);
				conn.setRequestProperty("X-Naver-Client-Secret", clientSecret);
			}
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
    
    /**
     * 모든 HTML 태그를 제거하고 반환한다.
     * 
     * @param html
     * @throws Exception  
     */
    public static String removeTag(String html) {
    	return html.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", "");
    }
}
