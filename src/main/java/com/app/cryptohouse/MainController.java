package com.app.cryptohouse;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.cryptohouse.Util.APIUrl;
import com.app.cryptohouse.Util.Exchange;
import com.app.cryptohouse.Util.TextUtil;
import com.app.cryptohouse.Util.Utils;


/**
 * Handles requests for the application home page.
 */
@Controller
public class MainController {
	
	private static final Logger logger = LoggerFactory.getLogger(MainController.class);
	
	private static String exchange, market, currency;
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model) {
		logger.info("Welcome home! The client locale is {}.", locale);
		
		Date date = new Date();
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
		
		String formattedDate = dateFormat.format(date);
		
		model.addAttribute("serverTime", formattedDate );
		
		return "home";
	}
	
	/**
	 * Home Kyboard API
	 * http://Server_Url/keyboard
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/keyboard", method = RequestMethod.GET, produces = "text/json; charset=UTF-8")
	@ResponseBody
	public String keyboard() {
		System.out.println("keyboard....");
		JSONObject obj = new JSONObject();
		obj.put("type"   , "buttons");
		obj.put("buttons", getBtnList());
		System.out.println("keyboard : "+obj.toJSONString());
		return obj.toJSONString();
	}

	/**
	 * 메시지 수신 및 자동응답 API
	 * @param obj
	 * @return
	 * @throws ParseException 
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/message", method = RequestMethod.POST, produces = "text/json; charset=UTF-8")
	@ResponseBody
	public String message(@RequestBody JSONObject obj) {
		System.out.println("message : "+obj.toJSONString());
		
		String user_key = (String) obj.get("user_key");
		String type     = (String) obj.get("type");
		String content  = (String) obj.get("content");
				
		JSONObject messageObj    = new JSONObject();
		JSONObject messageBtnObj = new JSONObject();
		JSONObject keyboardObj   = new JSONObject();
		JSONObject resObj        = new JSONObject();

		if(Exchange.빗썸.toString().equals(content) ||Exchange.코인원.toString().equals(content)) {
			exchange = content;
			market = "";
			messageObj.put("text", String.format("[ %s ]%s", content, "거래소를 선택하셨습니다.\n화폐단위를 '#' 과 함께 입력하세요.\n\n예시) #btc 또는 #eth 등"));
			resObj.put("message" , messageObj);
		}else if(Exchange.업비트.toString().equals(content) || Exchange.바이낸스.toString().equals(content)) {
			exchange = content;
			messageObj.put("text", String.format("[ %s ]%s", content, "거래소를 선택하셨습니다.\n아래의 버튼에서 마켓을 선택하세요."));
			keyboardObj.put("type"   , "buttons");
			if(Exchange.업비트.toString().equals(content)) { 
				keyboardObj.put("buttons", getBtnList_(content, new String[] {"BTC","KRW","ETH","USDT"}));
			}else {
				keyboardObj.put("buttons", getBtnList_(content, new String[] {"BNB","BTC","ETH","USDT"}));
			}
			resObj.put("message" , messageObj);
			resObj.put("keyboard", keyboardObj);
		}else if("뉴스보기".equals(content.replace(" ", "").trim())) {
			exchange=""; market=""; currency="";
			resObj = requestNaverNewsAPI(content);
		}else {
			if(content.contains("&")) {
				String[] data = content.split("&");
				market   = data[1]; 
				System.out.println("& : "+data[0]+", "+data[1]);
				messageObj.put("text", String.format("[ %s ]%s", market, "마켓을 선택하셨습니다.\n화폐단위를 '#' 과 함께 입력하세요.\n\n예시) #btc 또는 #eth 등"));
				resObj.put("message" , messageObj);
			}else if(content.contains("#")){
				System.out.println(exchange+", "+market+", "+content.replace("#", "").trim());
				if(exchange.equals(Exchange.업비트.toString())) {
					currency = content.replace("#", "").trim();
					resObj = requestUpbitAPI(currency, new String[] {market,currency});
				}else if(exchange.equals(Exchange.빗썸.toString())) {
					currency = content.replace("#", "").trim();
					resObj = requestBithumbAPI(currency, new String[] {market,currency});
				}else if(exchange.equals(Exchange.코인원.toString())) {
					currency = content.replace("#", "").trim();
					resObj = requestCoinoneAPI(currency, new String[] {market,currency});
				}else if(exchange.equals(Exchange.바이낸스.toString())) {
					currency = content.replace("#", "").trim();
					resObj = requestBinanceAPI(currency, new String[] {market,currency});
				}
			}else {
				messageBtnObj.put("label", String.format("%s %s", exchange, TextUtil.MESSAGE_BTN_TAIL));
				messageBtnObj.put("url"  , Exchange.valueOf(exchange).getUrl());
				
				messageObj.put("text", String.format(TextUtil.NO_RESULT_COMMENT, content));
				messageObj.put("message_button", messageBtnObj);
				
				keyboardObj.put("type"   , "buttons");
				keyboardObj.put("buttons", getBtnList());
				
				resObj.put("message" , messageObj);
				resObj.put("keyboard", keyboardObj);
				
				exchange=""; market=""; currency="";
			}
		}
		
		return resObj.toJSONString();
	}
	
	/**
	 * 업비트 api 호출
	 * @param content
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private JSONObject requestUpbitAPI(String content, String[] data) {
		System.out.println("Data : "+data[0]+", "+data[1]);
		String url = String.format("%s/%s/%s?code=CRIX.UPBIT.%s-%s&count=%s&to=%s", APIUrl.UPBIT_API_URL, "minutes", "1", data[0].toUpperCase().trim(), data[1].toUpperCase().trim(), "1", "");
		System.out.println("url : "+url);
		String result = Utils.requestAPI(url);
		
		JSONObject messageObj    = new JSONObject();
		JSONObject messageBtnObj = new JSONObject();
		JSONObject keyboardObj   = new JSONObject();
		JSONObject resObj        = new JSONObject();
		
		if("".equals(result) || null == result) {
			messageBtnObj.put("label", String.format("%s %s", Exchange.업비트.toString(), TextUtil.MESSAGE_BTN_TAIL));
			messageBtnObj.put("url"  , Exchange.valueOf(Exchange.업비트.toString()).getUrl());
			
			messageObj.put("text", String.format(TextUtil.NO_RESULT_COMMENT, content));
			messageObj.put("message_button", messageBtnObj);
			
			keyboardObj.put("type"   , "buttons");
			keyboardObj.put("buttons", getBtnList());
			
			resObj.put("message" , messageObj);
			resObj.put("keyboard", keyboardObj);
			
			exchange=""; market=""; currency="";
		}else if(!"".equals(result) && null != result){
			JSONParser parser = new JSONParser();
			result = result.replace("[", "").replace("]", "");
			
			JSONObject jsonObj = null;
			try {
				jsonObj = (JSONObject) parser.parse(result);
				
				String unit = getUint(data[0].trim());

				StringBuilder sb = new StringBuilder();
				if(jsonObj != null) {
//					sb.append("\nㆍ 종목코드 \n => "+jsonObj.get("code").toString())
					sb.append("\nㆍ 시가 : "  +jsonObj.get("openingPrice").toString().concat(unit))
					.append("\nㆍ 최고가  : " +jsonObj.get("highPrice").toString().concat(unit))
					.append("\nㆍ 최저가  : " +jsonObj.get("lowPrice").toString().concat(unit))
					.append("\nㆍ 종가  : "  +jsonObj.get("tradePrice").toString().concat(unit))
					.append("\nㆍ 거래대금  : "+jsonObj.get("candleAccTradePrice").toString().concat(unit))
					.append("\nㆍ 거래량  : " +jsonObj.get("candleAccTradeVolume").toString().toString().concat(" "+data[1].toUpperCase().trim()))
					.append("\n");
				}else {
					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, data[1].toUpperCase().trim()));
				}
				
				messageBtnObj.put("label", String.format("%s %s", Exchange.업비트.toString(), TextUtil.MESSAGE_BTN_TAIL));
				messageBtnObj.put("url"  , Exchange.valueOf(Exchange.업비트.toString()).getUrl());

				messageObj.put("text", String.format("[ %s ]\n%s %s \n %s ", Exchange.업비트.toString()+"거래소", "\n - "+data[0].toUpperCase().trim()+"마켓 / ", data[1].toUpperCase().trim()+"화폐 정보 -", sb.toString()));		
				messageObj.put("message_button", messageBtnObj);
				
				keyboardObj.put("type"   , "buttons");
				keyboardObj.put("buttons", getBtnList());
				
				resObj.put("message" , messageObj);
				resObj.put("keyboard", keyboardObj);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("resObj : "+resObj.toJSONString());

		return resObj;
	}
	
	/**
	 * 빗썸 api 호출
	 * @param content
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private JSONObject requestBithumbAPI(String content, String[] data) {
		System.out.println("requestAPI : "+content);
		System.out.println("Data : "+data[0]+", "+data[1]);
		String url = String.format(APIUrl.BITHUMB_API_URL, data[1].toUpperCase().trim());
		System.out.println("url : "+url);
		String result = Utils.requestAPI(url);

		JSONObject messageObj    = new JSONObject();
		JSONObject messageBtnObj = new JSONObject();
		JSONObject keyboardObj   = new JSONObject();
		JSONObject resObj        = new JSONObject();
		
		if("".equals(result) || null == result) {
			messageBtnObj.put("label", String.format("%s %s", Exchange.빗썸.toString(), TextUtil.MESSAGE_BTN_TAIL));
			messageBtnObj.put("url"  , Exchange.valueOf(Exchange.빗썸.toString()).getUrl());
			
			messageObj.put("text", String.format(TextUtil.NO_RESULT_COMMENT, content));
			messageObj.put("message_button", messageBtnObj);
			
			keyboardObj.put("type"   , "buttons");
			keyboardObj.put("buttons", getBtnList());
			
			resObj.put("message" , messageObj);
			resObj.put("keyboard", keyboardObj);
			
			exchange=""; market=""; currency="";
		}else if(!"".equals(result) && null != result){
			JSONParser parser = null;
			JSONObject jsonObj = null, jsonObj_ = null;
			try {
				parser = new JSONParser();
				jsonObj = (JSONObject) parser.parse(result);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println("status : "+jsonObj.get("status").toString());
			System.out.println("data : "+jsonObj.get("data").toString());
			
			String status = jsonObj.get("status").toString();
			String data_  = jsonObj.get("data").toString();
			try {
				parser = new JSONParser();
				jsonObj_ = (JSONObject) parser.parse(data_);
				
				String unit = getUint("krw");

				StringBuilder sb = new StringBuilder();
				if("0000".equals(status) && jsonObj != null) {
					sb.append("\nㆍ 시가  : "  +jsonObj_.get("opening_price").toString().concat(unit))
					.append("\nㆍ 최고가 : " +jsonObj_.get("max_price").toString().concat(unit))
					.append("\nㆍ 최저가  : " +jsonObj_.get("min_price").toString().concat(unit))
					.append("\nㆍ 종가  : "  +jsonObj_.get("closing_price").toString().concat(unit))
					.append("\nㆍ 평균가  : "  +jsonObj_.get("average_price").toString().concat(unit))
					.append("\nㆍ 거래량  : " +jsonObj_.get("units_traded").toString().toString().concat(" "+data[1].toUpperCase().trim()))
					.append("\n");
				}else {
					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, data[1].toUpperCase().trim()));
				}
				
				messageBtnObj.put("label", String.format("%s %s", Exchange.빗썸.toString(), TextUtil.MESSAGE_BTN_TAIL));
				messageBtnObj.put("url"  , Exchange.valueOf(Exchange.빗썸.toString()).getUrl());

				messageObj.put("text", String.format("[ %s ]\n%s \n %s ", Exchange.빗썸.toString()+"거래소", "\n - "+data[1].toUpperCase().trim()+" 화폐 정보 -", sb.toString()));		
				messageObj.put("message_button", messageBtnObj);
				
				keyboardObj.put("type"   , "buttons");
				keyboardObj.put("buttons", getBtnList());
				
				resObj.put("message" , messageObj);
				resObj.put("keyboard", keyboardObj);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

		}
		System.out.println("resObj : "+resObj.toJSONString());

		return resObj;
	}	
	
	/**
	 * 코인원 api 호출
	 * @param content
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private JSONObject requestCoinoneAPI(String content, String[] data) {
		System.out.println("content : "+content);
		System.out.println("Data : "+data[0]+", "+data[1]);
		String url = String.format(APIUrl.COINONE_API_URL, data[1].toLowerCase().trim());
		System.out.println("url : "+url);
		String result = Utils.requestAPI(url);
		System.out.println("result : "+result);
		JSONObject messageObj    = new JSONObject();
		JSONObject messageBtnObj = new JSONObject();
		JSONObject keyboardObj   = new JSONObject();
		JSONObject resObj        = new JSONObject();
		
		if("".equals(result) || null == result) {
			messageBtnObj.put("label", String.format("%s %s", Exchange.코인원.toString(), TextUtil.MESSAGE_BTN_TAIL));
			messageBtnObj.put("url"  , Exchange.valueOf(Exchange.코인원.toString()).getUrl());
			
			messageObj.put("text", String.format(TextUtil.NO_RESULT_COMMENT, content));
			messageObj.put("message_button", messageBtnObj);
			
			keyboardObj.put("type"   , "buttons");
			keyboardObj.put("buttons", getBtnList());
			
			resObj.put("message" , messageObj);
			resObj.put("keyboard", keyboardObj);
			
			exchange=""; market=""; currency="";
		}else if(!"".equals(result) && null != result){
			try {
				JSONParser parser  = new JSONParser();
				JSONObject jsonObj = (JSONObject) parser.parse(result);
				
				String unit = getUint("krw");

				String result_ = jsonObj.get("result").toString();
				StringBuilder sb = new StringBuilder();
				if("success".equals(result_) && jsonObj != null) {
					sb.append("\nㆍ 시가  : "  +jsonObj.get("first").toString().concat(unit))
					.append("\nㆍ 최고가  : " +jsonObj.get("high").toString().concat(unit))
					.append("\nㆍ 최저가  : " +jsonObj.get("low").toString().concat(unit))
					.append("\nㆍ 종가  : "  +jsonObj.get("last").toString().concat(unit))
					.append("\nㆍ 거래량  : " +jsonObj.get("volume").toString().toString().concat(" "+data[1].toUpperCase().trim()))
					.append("\n");
				}else {
					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, data[1].toUpperCase().trim()));
				}
				
				messageBtnObj.put("label", String.format("%s %s", Exchange.코인원.toString(), TextUtil.MESSAGE_BTN_TAIL));
				messageBtnObj.put("url"  , Exchange.valueOf(Exchange.코인원.toString()).getUrl());

				messageObj.put("text", String.format("[ %s ]\n%s \n %s ", Exchange.코인원.toString()+"거래소", "\n - "+data[1].toUpperCase().trim()+" 화폐 정보 -", sb.toString()));		
				messageObj.put("message_button", messageBtnObj);
				
				keyboardObj.put("type"   , "buttons");
				keyboardObj.put("buttons", getBtnList());
				
				resObj.put("message" , messageObj);
				resObj.put("keyboard", keyboardObj);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("resObj : "+resObj.toJSONString());

		return resObj;
	}	
	
	/**
	 * 바이낸스 api 호출
	 * @param content
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private JSONObject requestBinanceAPI(String content, String[] data) {
		System.out.println("content : "+content);
		System.out.println("Data : "+data[0]+", "+data[1]);
		String url = String.format(APIUrl.BINANCE_API_URL, data[1].replace("?", "").toUpperCase().trim()+data[0].toUpperCase().trim());
		System.out.println("url : "+url);
		String result = Utils.requestAPI(url);
		System.out.println("result : "+result);
		JSONObject messageObj    = new JSONObject();
		JSONObject messageBtnObj = new JSONObject();
		JSONObject keyboardObj   = new JSONObject();
		JSONObject resObj        = new JSONObject();
		
		if("".equals(result) || null == result) {
			messageBtnObj.put("label", String.format("%s %s", Exchange.바이낸스.toString(), TextUtil.MESSAGE_BTN_TAIL));
			messageBtnObj.put("url"  , Exchange.valueOf(Exchange.바이낸스.toString()).getUrl());
			
			messageObj.put("text", String.format(TextUtil.NO_RESULT_COMMENT, content));
			messageObj.put("message_button", messageBtnObj);
			
			keyboardObj.put("type"   , "buttons");
			keyboardObj.put("buttons", getBtnList());
			
			resObj.put("message" , messageObj);
			resObj.put("keyboard", keyboardObj);
			
			exchange=""; market=""; currency="";
		}else if(!"".equals(result) && null != result){
			try {
				JSONParser parser  = new JSONParser();
				JSONObject jsonObj = (JSONObject) parser.parse(result);
				
				String unit = getUint(data[0].trim());
				String data1 = data[1].replace("?", "").toUpperCase().trim();
				
				StringBuilder sb = new StringBuilder();
				if(jsonObj != null) {
					sb.append("\nㆍ 시가  : "  +jsonObj.get("openPrice").toString().concat(unit))
					.append("\nㆍ 최고가  : " +jsonObj.get("highPrice").toString().concat(unit))
					.append("\nㆍ 최저가 : " +jsonObj.get("lowPrice").toString().concat(unit))
					.append("\nㆍ 종가 : "  +jsonObj.get("lastPrice").toString().concat(unit))
					.append("\nㆍ 거래량  : " +jsonObj.get("volume").toString().toString().concat(" "+data1))
					.append("\n");
				}else {
					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, data1));
				}
				
				messageBtnObj.put("label", String.format("%s %s", Exchange.바이낸스.toString(), TextUtil.MESSAGE_BTN_TAIL));
				messageBtnObj.put("url"  , Exchange.valueOf(Exchange.바이낸스.toString()).getUrl());

				messageObj.put("text", String.format("[ %s ]\n%s %s \n %s ", Exchange.바이낸스.toString()+"거래소", "\n - "+data[0].toUpperCase().trim()+"마켓 / ", data1+"화폐 정보 -", sb.toString()));	
				messageObj.put("message_button", messageBtnObj);
				
				keyboardObj.put("type"   , "buttons");
				keyboardObj.put("buttons", getBtnList());
				
				resObj.put("message" , messageObj);
				resObj.put("keyboard", keyboardObj);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("resObj : "+resObj.toJSONString());

		return resObj;
	}	
	
	@SuppressWarnings("unchecked")
	private JSONObject requestNaverNewsAPI(String content) {		
		String text = "";
		try {
			text = URLEncoder.encode("암호화폐", "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("text : "+text);
		String url = String.format(APIUrl.NAVER_NEWS_API_URL, text,"10","1","date");
		System.out.println("url : "+url);
		String result = Utils.requestAPI(url);
		System.out.println("result : "+result);
		JSONObject messageObj  = new JSONObject();
		JSONObject keyboardObj = new JSONObject();
		JSONObject resObj      = new JSONObject();
		
		if("".equals(result) || null == result) {
			messageObj.put("text", String.format(TextUtil.COMMENT, "뉴스"));
			resObj.put("message" , messageObj);
		}else if(!"".equals(result) && null != result){
			try {
				JSONParser parser  = new JSONParser();
				JSONObject jsonObj = (JSONObject) parser.parse(result);

				StringBuilder sb = new StringBuilder();
				if(jsonObj != null) {
					JSONArray array = (JSONArray) jsonObj.get("items");
					int length = array.size();
					for(int i=0; i<length; i++) {
						JSONObject json = (JSONObject) array.get(i);
						sb.append((i+1)+". "+Utils.removeTag(json.get("title").toString())+"\n");
						sb.append(json.get("link").toString()+"\n\n");
					}
					System.out.println("link : "+sb.toString());
				}else {
					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, "뉴스"));
				}

				messageObj.put("text", String.format("%s", sb.toString()));	
				
				keyboardObj.put("type"   , "buttons");
				keyboardObj.put("buttons", getBtnList());
				
				resObj.put("message" , messageObj);
				resObj.put("keyboard", keyboardObj);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("resObj : "+resObj.toJSONString());

		return resObj;
	}
	
//	@SuppressWarnings("unchecked")
//	private JSONObject requestCoinnestAPI(String content, String[] data) {
//		System.out.println("content : "+content);
//		System.out.println("Data : "+data[0]+", "+data[1]);
//		String url = String.format("%s%s", APIUrl.COINNEST_API_URL, data[1].toLowerCase().trim());
//		System.out.println("url : "+url);
//		String result = Utils.requestAPI(url);
//		System.out.println("result : "+result);
//		JSONObject messageObj    = new JSONObject();
//		JSONObject messageBtnObj = new JSONObject();
//		JSONObject keyboardObj   = new JSONObject();
//		JSONObject resObj        = new JSONObject();
//		
//		if("".equals(result) || null == result) {
//			messageObj.put("text", String.format(TextUtil.COMMENT, content));
//			resObj.put("message" , messageObj);
//		}else if(!"".equals(result) && null != result){
//			try {
//				JSONParser parser  = new JSONParser();
//				JSONObject jsonObj = (JSONObject) parser.parse(result);
//				
//				String unit = getUint("krw");
//
//				String result_ = jsonObj.get("result").toString();
//				StringBuilder sb = new StringBuilder();
//				if("success".equals(result_) && jsonObj != null) {
//					sb.append("\n● 시가 \n => "  +jsonObj.get("buy").toString().concat(unit))
//					.append("\n● 최고가 \n => " +jsonObj.get("high").toString().concat(unit))
//					.append("\n● 최저가 \n => " +jsonObj.get("low").toString().concat(unit))
//					.append("\n● 종가 \n => "  +jsonObj.get("last").toString().concat(unit))
//					.append("\n● 거래량 \n => " +jsonObj.get("vol").toString().toString());
//				}else {
//					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, data[1].toUpperCase()));
//				}
//				
//				messageBtnObj.put("label", String.format("%s %s", Exchange.코인네스트.toString(), TextUtil.MESSAGE_BTN_TAIL));
//				messageBtnObj.put("url"  , Exchange.valueOf(Exchange.코인네스트.toString()).getUrl());
//
//				messageObj.put("text", String.format("[%s]\n%s \n %s ", Exchange.코인네스트.toString(), data[1].toUpperCase().trim()+" 화폐 정보", sb.toString()));		
//				messageObj.put("message_button", messageBtnObj);
//				
//				keyboardObj.put("type"   , "buttons");
//				keyboardObj.put("buttons", getBtnList());
//				
//				resObj.put("message" , messageObj);
//				resObj.put("keyboard", keyboardObj);
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		System.out.println("resObj : "+resObj.toJSONString());
//
//		return resObj;
//	}	
	
	private String getUint(String param) {
		String unit = null;
		if("krw".equalsIgnoreCase(param))
			unit = " 원";
		else if("btc".equalsIgnoreCase(param))
			unit = " BTC";
		else if("eth".equalsIgnoreCase(param))
			unit = " ETH";		
		else if("usdt".equalsIgnoreCase(param))
			unit = " USDT";
		else if("bnb".equalsIgnoreCase(param))
			unit = " BNB";
		return unit;
	}
	
	private ArrayList<String> getBtnList(){
		ArrayList<String> list = new ArrayList<String>();
		int length = Exchange.values().length;
		for(int i=0; i<length; i++) {
			list.add(i, Exchange.getName(i));
		}

		return list;
	}
	
	private ArrayList<String> getBtnList_(String str, String[] arr){
		ArrayList<String> list = new ArrayList<String>();
		int length = arr.length;
		for(int i=0; i<length; i++) {
			list.add(i, str+"&"+arr[i]);
		}

		return list;
	}
	
}
