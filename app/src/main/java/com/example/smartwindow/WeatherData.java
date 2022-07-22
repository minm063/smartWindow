package com.example.smartwindow;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;

public class WeatherData extends Thread {

    private String weather = "";
    private String t1h = "";
    private String reh = "";
    private String rn1 = "";

    @Override
    public void run() {
        super.run();
    }

    public String lookUpWeather(String baseDate, String baseTime, String nx, String ny) throws IOException, JSONException {

        String type = "json";
        String apiUrl = "";
        String serviceKey = "";

        /*
         * GET방식으로 전송해서 파라미터 받아오기
         */
        String urlBuilder = apiUrl + "?" + URLEncoder.encode("ServiceKey", "UTF-8") + "=" + serviceKey +
                "&" + URLEncoder.encode("nx", "UTF-8") + "=" + URLEncoder.encode(nx, "UTF-8") + //경도
                "&" + URLEncoder.encode("ny", "UTF-8") + "=" + URLEncoder.encode(ny, "UTF-8") + //위도
                "&" + URLEncoder.encode("base_date", "UTF-8") + "=" + URLEncoder.encode(baseDate, "UTF-8") + // 조회하고싶은 날짜
                "&" + URLEncoder.encode("base_time", "UTF-8") + "=" + URLEncoder.encode(baseTime, "UTF-8") + // 조회하고싶은 시간
                "&" + URLEncoder.encode("dataType", "UTF-8") + "=" + URLEncoder.encode(type, "UTF-8");// 타입
        URL url = new URL(urlBuilder);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();
        String result = sb.toString();


        // response 키를 가지고 데이터를 파싱
        JSONObject jsonObj_1 = new JSONObject(result);
        String response = jsonObj_1.optString("response");

        // response 로 부터 body 찾기
        JSONObject jsonObj_2 = new JSONObject(response);
        String body = jsonObj_2.optString("body");

        // body 로 부터 items 찾기
        JSONObject jsonObj_3 = new JSONObject(body);
        String items = jsonObj_3.optString("items");
        Log.i("ITEMS", items);

        // items로 부터 itemlist 를 받기
        JSONObject jsonObj_4 = new JSONObject(items);
        JSONArray jsonArray = jsonObj_4.getJSONArray("item");

        for (int i = 0; i < jsonArray.length(); i++) {
            jsonObj_4 = jsonArray.getJSONObject(i);
            String value = jsonObj_4.optString("obsrValue");
            String category = jsonObj_4.optString("category");

            if (category.equals("PTY")) {
                weather = value;
            }
            if (category.equals("T3H") || category.equals("T1H")) {
                t1h = "기온 " + value + "℃";
            }
            if (category.equals("REH")) {
                reh = "습도 " + value + "%";
            }
            if (category.equals("RN1")) {
                rn1 = "1시간 강수량 " + value + "mm";
            }

        }

        return weather + ", " + t1h + ", " + reh + ", " + rn1;
    }

}
