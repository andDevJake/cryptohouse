package com.app.cryptohouse;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


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
		
		return obj.toJSONString();
	}

	/**
	 * �޽��� ���� �� �ڵ����� API
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
				
		JSONObject messageObj = new JSONObject();
		JSONObject resObj     = new JSONObject();
	
		if(Exchange.����Ʈ.toString().equals(content)) {
			messageObj.put("text", String.format("[%s]%s", content, "�� �����ϼ̽��ϴ�.\n���Ͻô� ����&ȭ������� �Է��ϼ���.\n\n����)\n 1.btc&eth\n 2.krw&btc\n 3.eth&ada\n 4.usdt&xrp"));
			resObj.put("message" , messageObj);
		}else if(Exchange.����.toString().equals(content)) {
			messageObj.put("text", String.format("[%s]%s", content, "�� �����ϼ̽��ϴ�.\n���Ͻô� ȭ������� '&'�ڿ� �Է��ϼ���.\n('&'���� 'umb'�� �ʼ��Դϴ�.)\n\n����)\n 1.umb&btc\n 2.umb&eth"));
			resObj.put("message" , messageObj);
		}else if(Exchange.���ο�.toString().equals(content)) {
			messageObj.put("text", String.format("[%s]%s", content, "�� �����ϼ̽��ϴ�.\n���Ͻô� ȭ������� '&'�ڿ� �Է��ϼ���.\n('&'���� 'one'�� �ʼ��Դϴ�.)\n\n����)\n 1.one&btc\n 2.one&eth"));
			resObj.put("message" , messageObj);
		}else if(Exchange.���̳���.toString().equals(content)) {
			messageObj.put("text", String.format("[%s]%s", content, "�� �����ϼ̽��ϴ�.\n���Ͻô� ����&ȭ������� �Է��ϼ���.\n('?'�� �ʼ��Դϴ�.)\n\n����)\n 1.btc&eth?\n 2.bnb&neo?\n 3.eth&ada?\n 4.usdt&xrp?"));
			resObj.put("message" , messageObj);
		}else {
			System.out.println("content : "+content);
			if(!content.contains("&")) {
				messageObj.put("text", String.format(TextUtil.COMMENT, content));
				resObj.put("message" , messageObj);
			}else {
				String[] data = null;
				if(content.startsWith("&") || content.endsWith("&")) {
					messageObj.put("text", String.format(TextUtil.COMMENT, content));
					resObj.put("message" , messageObj);
				}else {
					data = content.split("&");
					if(content.contains("umb")) {
						resObj = requestBithumbAPI(content, data); // response �޾ƾ� ��..
					}else if(content.contains("one")) {
						resObj = requestCoinoneAPI(content, data); // response �޾ƾ� ��..
					}else if(content.contains("?")){
						resObj = requestBinanceAPI(content, data); // response �޾ƾ� ��..
					}else {
						resObj = requestUpbitAPI(content, data); // response �޾ƾ� ��..
					}
				}
			}  
		}
		
		return resObj.toJSONString();
	}
	
	/**
	 * ����Ʈ api ȣ��
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
			messageObj.put("text", String.format(TextUtil.COMMENT, content));
			resObj.put("message" , messageObj);
		}else if(!"".equals(result) && null != result){
			JSONParser parser = new JSONParser();
			result = result.replace("[", "").replace("]", "");
			
			JSONObject jsonObj = null;
			try {
				jsonObj = (JSONObject) parser.parse(result);
				
				String unit = getUint(data[0].trim());

				StringBuilder sb = new StringBuilder();
				if(jsonObj != null) {
					sb.append("\n�� �����ڵ� \n => "+jsonObj.get("code").toString())
					.append("\n�� �ð� \n => "  +jsonObj.get("openingPrice").toString().concat(unit))
					.append("\n�� �ְ� \n => " +jsonObj.get("highPrice").toString().concat(unit))
					.append("\n�� ������ \n => " +jsonObj.get("lowPrice").toString().concat(unit))
					.append("\n�� ���� \n => "  +jsonObj.get("tradePrice").toString().concat(unit))
					.append("\n�� �ŷ���� \n => "+jsonObj.get("candleAccTradePrice").toString().concat(unit))
					.append("\n�� �ŷ��� \n => " +jsonObj.get("candleAccTradeVolume").toString().toString().concat(" "+data[1].toUpperCase().trim()));
				}else {
					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, data[1].toUpperCase().trim()));
				}
				
				messageBtnObj.put("label", String.format("%s %s", Exchange.����Ʈ.toString(), TextUtil.MESSAGE_BTN_TAIL));
				messageBtnObj.put("url"  , Exchange.valueOf(Exchange.����Ʈ.toString()).getUrl());

				messageObj.put("text", String.format("[%s]\n%s %s \n %s ", Exchange.����Ʈ.toString(), data[0].toUpperCase().trim()+"���� / ", data[1].toUpperCase().trim()+"ȭ�� ����", sb.toString()));		
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
	 * ���� api ȣ��
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
			messageObj.put("text", String.format(TextUtil.COMMENT, content));
			resObj.put("message" , messageObj);
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
					sb.append("\n�� �ð� \n => "  +jsonObj_.get("opening_price").toString().concat(unit))
					.append("\n�� �ְ� \n => " +jsonObj_.get("max_price").toString().concat(unit))
					.append("\n�� ������ \n => " +jsonObj_.get("min_price").toString().concat(unit))
					.append("\n�� ���� \n => "  +jsonObj_.get("closing_price").toString().concat(unit))
					.append("\n�� ��հ� \n => "  +jsonObj_.get("average_price").toString().concat(unit))
					.append("\n�� �ŷ��� \n => " +jsonObj_.get("units_traded").toString().toString().concat(" "+data[1].toUpperCase().trim()));
				}else {
					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, data[1].toUpperCase().trim()));
				}
				
				messageBtnObj.put("label", String.format("%s %s", Exchange.����.toString(), TextUtil.MESSAGE_BTN_TAIL));
				messageBtnObj.put("url"  , Exchange.valueOf(Exchange.����.toString()).getUrl());

				messageObj.put("text", String.format("[%s]\n%s \n %s ", Exchange.����.toString(), data[1].toUpperCase().trim()+" ȭ�� ����", sb.toString()));		
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
	 * ���ο� api ȣ��
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
			messageObj.put("text", String.format(TextUtil.COMMENT, content));
			resObj.put("message" , messageObj);
		}else if(!"".equals(result) && null != result){
			try {
				JSONParser parser  = new JSONParser();
				JSONObject jsonObj = (JSONObject) parser.parse(result);
				
				String unit = getUint("krw");

				String result_ = jsonObj.get("result").toString();
				StringBuilder sb = new StringBuilder();
				if("success".equals(result_) && jsonObj != null) {
					sb.append("\n�� �ð� \n => "  +jsonObj.get("first").toString().concat(unit))
					.append("\n�� �ְ� \n => " +jsonObj.get("high").toString().concat(unit))
					.append("\n�� ������ \n => " +jsonObj.get("low").toString().concat(unit))
					.append("\n�� ���� \n => "  +jsonObj.get("last").toString().concat(unit))
					.append("\n�� �ŷ��� \n => " +jsonObj.get("volume").toString().toString().concat(" "+data[1].toUpperCase().trim()));
				}else {
					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, data[1].toUpperCase().trim()));
				}
				
				messageBtnObj.put("label", String.format("%s %s", Exchange.���ο�.toString(), TextUtil.MESSAGE_BTN_TAIL));
				messageBtnObj.put("url"  , Exchange.valueOf(Exchange.���ο�.toString()).getUrl());

				messageObj.put("text", String.format("[%s]\n%s \n %s ", Exchange.���ο�.toString(), data[1].toUpperCase().trim()+" ȭ�� ����", sb.toString()));		
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
	 * ���̳��� api ȣ��
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
			messageObj.put("text", String.format(TextUtil.COMMENT, content));
			resObj.put("message" , messageObj);
		}else if(!"".equals(result) && null != result){
			try {
				JSONParser parser  = new JSONParser();
				JSONObject jsonObj = (JSONObject) parser.parse(result);
				
				String unit = getUint(data[0].trim());
				String data1 = data[1].replace("?", "").toUpperCase().trim();
				
				StringBuilder sb = new StringBuilder();
				if(jsonObj != null) {
					sb.append("\n�� �ð� \n => "  +jsonObj.get("openPrice").toString().concat(unit))
					.append("\n�� �ְ� \n => " +jsonObj.get("highPrice").toString().concat(unit))
					.append("\n�� ������ \n => " +jsonObj.get("lowPrice").toString().concat(unit))
					.append("\n�� ���� \n => "  +jsonObj.get("lastPrice").toString().concat(unit))
					.append("\n�� �ŷ��� \n => " +jsonObj.get("volume").toString().toString().concat(" "+data1));
				}else {
					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, data1));
				}
				
				messageBtnObj.put("label", String.format("%s %s", Exchange.���̳���.toString(), TextUtil.MESSAGE_BTN_TAIL));
				messageBtnObj.put("url"  , Exchange.valueOf(Exchange.���̳���.toString()).getUrl());

				messageObj.put("text", String.format("[%s]\n%s %s \n %s ", Exchange.���̳���.toString(), data[0].toUpperCase().trim()+"���� / ", data1+"ȭ�� ����", sb.toString()));	
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
//					sb.append("\n�� �ð� \n => "  +jsonObj.get("buy").toString().concat(unit))
//					.append("\n�� �ְ� \n => " +jsonObj.get("high").toString().concat(unit))
//					.append("\n�� ������ \n => " +jsonObj.get("low").toString().concat(unit))
//					.append("\n�� ���� \n => "  +jsonObj.get("last").toString().concat(unit))
//					.append("\n�� �ŷ��� \n => " +jsonObj.get("vol").toString().toString());
//				}else {
//					sb.append(String.format(TextUtil.NO_RESULT_COMMENT, data[1].toUpperCase()));
//				}
//				
//				messageBtnObj.put("label", String.format("%s %s", Exchange.���γ׽�Ʈ.toString(), TextUtil.MESSAGE_BTN_TAIL));
//				messageBtnObj.put("url"  , Exchange.valueOf(Exchange.���γ׽�Ʈ.toString()).getUrl());
//
//				messageObj.put("text", String.format("[%s]\n%s \n %s ", Exchange.���γ׽�Ʈ.toString(), data[1].toUpperCase().trim()+" ȭ�� ����", sb.toString()));		
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
			unit = " ��";
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
	
}
