package com.example.smartwindow;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;


public class MainActivity extends AppCompatActivity {

    SwitchCompat fSwitch;
    ImageButton window;
    TextView T1H, REH, RN1, PTY, wSensor, dSensor;
    ImageView imageView;
    String[] temp;
    String onOff = "on";
    int cnt;
    String state;
    private String x = "", y = "", date = "", weather = "";
    private String baseTime, now_state;
    private Long mLastClickTime = 0L;

    long mNow;
    Date mDate;
    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat mFormat = new SimpleDateFormat("yyyyMMdd HHmm", Locale.KOREAN);
    private SwipeRefreshLayout swipeRefreshLayout;
    Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String addr = intent.getStringExtra("ip");
        int port = Integer.parseInt(intent.getStringExtra("port"));
        now_state = intent.getStringExtra("now_state");

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        fSwitch = findViewById(R.id.functionSwitch);
        window = findViewById(R.id.window);
        T1H = findViewById(R.id.T1H);
        REH = findViewById(R.id.REH);
        RN1 = findViewById(R.id.RN1);
        PTY = findViewById(R.id.PTY);
        imageView = findViewById(R.id.weather);
        wSensor = findViewById(R.id.wSensor);
        dSensor = findViewById(R.id.dSensor);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        mHandler = new Handler();

        GpsTracker gpsTracker = new GpsTracker(this);
        double lat = gpsTracker.getLatitude();
        double lon = gpsTracker.getLongitude();

        String address = getCurrentAddress(lat, lon);
        Log.i("address", address);
        String[] local = address.split(", ");
        String[] local2 = local[2].split(" ");
        String localName = local2[0];
        Log.i("localName", localName);
//        readExcel(localName);
        // 한글이 깨져서 읽힘
        x = "59";
        y = "74";

        String tm = getTime();
        date = tm.split(" ")[0];
        String time = tm.split(" ")[1];

        baseTime = time;
        if (Integer.parseInt(time) % 100 < 40) {
            int t = Integer.parseInt(time.substring(0, 2));

            if (t == 0) {
                baseTime = "2300";
            } else {
                t -= 1;
                if (t < 10) {
                    baseTime = "0" + t + "00";
                } else {
                    baseTime = t + "00";
                }
            }
        }

        mWeather();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            // 새로고침
            MyClientTask myClientTask = new MyClientTask(addr, port, "swipe");
            myClientTask.execute();

            mWeather();
            if (now_state.equals("0")) {
                Toast.makeText(getApplicationContext(), "창문이 닫혔습니다.", Toast.LENGTH_SHORT).show();
                window.setBackgroundResource(R.drawable.close);
            }
            swipeRefreshLayout.setRefreshing(false);
        });

        fSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(getApplicationContext(), "자동 개폐 기능 ON", Toast.LENGTH_SHORT).show();
                onOff = "on";
            } else {
                Toast.makeText(getApplicationContext(), "자동 개폐 기능 OFF", Toast.LENGTH_SHORT).show();
                onOff = "off";
            }
            MyClientTask myClientTask = new MyClientTask(addr, port, onOff);
            myClientTask.execute();
        });

        cnt = 0;
        if (now_state.equals("1")) {
            window.setBackgroundResource(R.drawable.open);
            state = "close";
        } else if (now_state.equals("0")) {
            window.setBackgroundResource(R.drawable.close);
            state = "open";
        } else {
            Toast.makeText(getApplicationContext(), "연결 오류", Toast.LENGTH_SHORT).show();
        }
        // 초기 : 열린 상태(1), 닫힌 상태(0)
        window.setOnClickListener(v -> {

            MyClientTask myClientTask = new MyClientTask(addr, port, state);
            myClientTask.execute();

            Toast.makeText(getApplicationContext(), "동기화중입니다", Toast.LENGTH_LONG).show();
//                if (SystemClock.elapsedRealtime() - mLastClickTime > 5000) {
//
//                    if (now_state.equals("1")) {
//                        state = "close";
//                        v.setBackgroundResource(R.drawable.open);
//                    } else {
//                        state = "open";
//                        v.setBackgroundResource(R.drawable.close);
//                    }
//                    mLastClickTime = SystemClock.elapsedRealtime();
//                } else {
//                    Toast.makeText(getApplicationContext(), "창문을 닫는 중입니다.", Toast.LENGTH_SHORT).show();
//                }

        });
    }

    private void onRun() {
        if (onOff.equals("on")) {
//            if (now_state.equals("1")) { // now_state가 열린 상태, 1
//                window.setBackgroundResource(R.drawable.close);
//                state = "open";
//            } else if (now_state.equals("0")) { // 닫힌 상태
//                window.setBackgroundResource(R.drawable.open);
//                state = "close";
//            }
            if (now_state.equals("1")) {
                window.setBackgroundResource(R.drawable.open);
                state = "close";
            } else if (now_state.equals("0")) {
                window.setBackgroundResource(R.drawable.close);
                state = "open";
            } else {
                Toast.makeText(getApplicationContext(), "연결 오류", Toast.LENGTH_SHORT).show();
            }
            Log.i("now_state", now_state);
            Log.i("state", state);
        }
    }

    private void mWeather() {
        try {
            WeatherData weatherData = new WeatherData();
            weatherData.start();
            weather = weatherData.lookUpWeather(date, baseTime, x, y);
            temp = weather.split(", ");
            weatherData.join();

        } catch (InterruptedException | JSONException | IOException e) {
            e.printStackTrace();
        }

//        new Thread(() -> {
//            try {
//                weather = WeatherData.lookUpWeather(date, baseTime, x, y);
//                temp = weather.split(", ");
//            } catch (IOException e) {
//                Log.i("wd error1", e.getMessage());
//            } catch (JSONException e) {
//                Log.i("wd error2", e.getMessage());
//            }
//        }).start();
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                setWeather();
//            }
//        }, 2000);
        setWeather(temp);
    }

    private void setWeather(String[] temp) {
        Log.i("lookupWeather", Arrays.toString(temp));

        switch (temp[0]) {
            case "0":
                weather = "맑음";
                PTY.setText(weather);
                imageView.setImageResource(R.drawable.day0);
                break;
            case "1":
                weather = "비";
                PTY.setText(weather);
                imageView.setImageResource(R.drawable.day1);
                break;
            case "2":
                weather = "비 또는 눈";
                PTY.setText(weather);
                imageView.setImageResource(R.drawable.day2);
                break;
            case "3":
                weather = "눈";
                PTY.setText(weather);
                imageView.setImageResource(R.drawable.day3);
                break;
            case "4":
                weather = "소나기";
                PTY.setText(weather);
                imageView.setImageResource(R.drawable.day4);
                break;
            case "5":
                weather = "빗방울";
                PTY.setText(weather);
                imageView.setImageResource(R.drawable.day5);
                break;
            case "6":
                weather = "빗방울 또는 눈날림";
                PTY.setText(weather);
                imageView.setImageResource(R.drawable.day6);
                break;
            case "7":
                weather = "눈날림";
                PTY.setText(weather);
                imageView.setImageResource(R.drawable.day7);
                break;
        }
        T1H.setText(temp[1]);
        REH.setText(temp[2]);
        RN1.setText(temp[3]);
    }

    private String getTime() {
        TimeZone tz;
        tz = TimeZone.getTimeZone("Asia/Seoul");
        mFormat.setTimeZone(tz);

        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }

    public String getCurrentAddress(double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0) + "\n";
    }

    public void readExcel(String localName) {
        try {
            InputStream is = getBaseContext().getResources().getAssets().open("local_name.xls");
            Workbook wb = Workbook.getWorkbook(is);

            if (wb != null) {
                Sheet sheet = wb.getSheet(0);   // 시트 불러오기
                if (sheet != null) {
                    int colTotal = sheet.getColumns();    // 전체 컬럼
                    int rowIndexStart = 1;                  // row 인덱스 시작
                    int rowTotal = sheet.getColumn(colTotal - 1).length;

                    for (int row = rowIndexStart; row < rowTotal; row++) {
                        String contents = sheet.getCell(0, row).getContents();
                        if (contents.contains(localName)) {
                            x = sheet.getCell(1, row).getContents();
                            y = sheet.getCell(2, row).getContents();
                            row = rowTotal;
                        }
                    }
                }
            }
        } catch (IOException | BiffException | StringIndexOutOfBoundsException e) {
            Log.i("READ_EXCEL1", e.getMessage());
            e.printStackTrace();
        }
        // x, y = String형 전역변수
        Log.i("격자값", "x = " + x + "  y = " + y);
    }

    // AsyncTask
    public class MyClientTask extends AsyncTask<Void, Void, Void> {
        String dstAddress;
        int dstPort;
        String response = "", alert = "";
        String myMessage;

        //constructor
        MyClientTask(String address, int port, String message) {
            dstAddress = address;
            dstPort = port;
            myMessage = message;
        }


        @Override
        protected Void doInBackground(Void... arg0) {

            Socket socket = null;

            try {
                socket = new Socket(dstAddress, dstPort);

                //송신
                OutputStream out = socket.getOutputStream();
                out.write(myMessage.getBytes());

                //수신
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];
                int bytesRead;
                InputStream inputStream = socket.getInputStream();
                /*
                 * notice:
                 * inputStream.read() will block if no data return
                 */
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    response += byteArrayOutputStream.toString("UTF-8");
                }

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                alert = "UnknownHostException: " + e;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                alert = "IOException: " + e;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        alert = "IOException: " + e;
                    }
                }
            }
            return null;
        }

        // 수행되던 작업이 종료됐을 때
        @Override
        protected void onPostExecute(Void result) {

            String[] res = response.split(" ");
            wSensor.setText(res[0]);
            dSensor.setText(res[1]);
            now_state = res[res.length - 1];

//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    onRun();
//                }
//            }, 1000);

            Log.i("state2", now_state);
            super.onPostExecute(result);
        }
    }
}