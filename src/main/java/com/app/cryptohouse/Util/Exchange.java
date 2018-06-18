package com.app.cryptohouse.Util;

public enum Exchange {
	
	  업비트("https://upbit.com/")
	, 빗썸("https://www.bithumb.com/")
	, 코인원("https://coinone.co.kr/")
	, 바이낸스("https://www.binance.com/");
	
//	, 코인네스트("https://www.coinnest.co.kr/"); => 응답이 제대로 안와서 생략...

	private String url;
	
	Exchange() {
		
	}
	
	private Exchange(String url) {
		this.url = url;
	}
	
	public String getUrl() {
		return url;
	}

	public static String getName(int i) {
		return Exchange.values()[i].name();
	}
	
	public static int getOrdinal(String name) {
		return Exchange.valueOf(name).ordinal();
	}

}
