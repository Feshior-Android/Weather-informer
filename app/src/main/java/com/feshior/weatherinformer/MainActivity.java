package com.feshior.weatherinformer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.AsyncTaskLoader;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private EditText cityName;
    private Button getWeather;
    private Button getWeatherForTomorrow;
    private TextView resultInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityName = findViewById(R.id.city_name);
        getWeather = findViewById(R.id.get_weather);
        getWeatherForTomorrow = findViewById(R.id.get_weather_for_tomorrow);
        resultInfo = findViewById(R.id.result_info);

        ButtonListener buttonListener = new ButtonListener();

        getWeatherForTomorrow.setOnClickListener(buttonListener);
        getWeather.setOnClickListener(buttonListener);

//        getWeather.setOnClickListener(view -> {
//            if(cityName.getText().equals("")){
//                Toast.makeText(MainActivity.this, R.string.empty_city_message, Toast.LENGTH_SHORT).show();
//            }else{
//                String key = "76e51799ebf2483696f211428220909";
//                String pattern = "https://api.weatherapi.com/v1/current.json?key=%s&q=%s";
//                String api = String.format(pattern, key, cityName.getText());
//                ApiDataParser apiDataParser = new ApiDataParser();
//                apiDataParser.onPreExecute();
//                try {
//                    apiDataParser.execute(api);
//                   // String result = apiDataParser.doInBackground();
//                    //apiDataParser.onPostExecute(result);
//                }catch (Exception ex){
//                    ex.printStackTrace();
//                }
//            }
//        });
    }

    protected class ButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.get_weather:
                    getDataFromApi(0);
                    break;
                case R.id.get_weather_for_tomorrow:
                    getDataFromApi(1);
                    break;
                default:
                    break;
            }
        }
    }

    private void getDataFromApi(int days) {
        if (cityName.getText().equals("")) {
            Toast.makeText(MainActivity.this, R.string.empty_city_message, Toast.LENGTH_SHORT).show();
        } else {
            String key = "76e51799ebf2483696f211428220909";
            String api;
            if (days <= 0) {
                String pattern = "https://api.weatherapi.com/v1/current.json?key=%s&q=%s";
                api = String.format(pattern, key, cityName.getText());
            } else {
                String pattern = "https://api.weatherapi.com/v1/forecast.json?key=%s&q=%s&days=%d";
                api = String.format(pattern, key, cityName.getText(), days);
            }

            ApiDataParser apiDataParser = new ApiDataParser();
            try {
                apiDataParser.onPreExecute();
                apiDataParser.execute(api);
            } catch (Exception ex) {
                resultInfo.setText(R.string.request_err_message);
                ex.printStackTrace();

            }
        }
    }


    @SuppressLint("StaticFieldLeak")
    private class ApiDataParser extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();
            resultInfo.setText(R.string.preload_message);
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                // ->
                String line = "";
                StringBuffer buffer = new StringBuffer();

                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append('\n');
                }
                return buffer.toString();

            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (connection != null)
                    connection.disconnect();
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                    }
                }
            }
            return null;
        }

        protected void onPostExecute(String result) {
            if (result == null) {
                resultInfo.setText(R.string.err_load_message);
            } else {
                try {
                    JSONObject json = new JSONObject(result);

                    if (!json.has("forecast"))
                        resultInfo.setText(parseCurrent(json));
                    else
                        resultInfo.setText(parseForecast(json));

                } catch (JSONException ex) {
                    ex.printStackTrace();
                }

            }
        }

        private String parseCurrent(JSONObject json) throws JSONException {
            String info = "";
            info += String.format("Weather in %s \n",
                    json.getJSONObject("location").getString("name"));

            info += String.format("Country: %s%n",
                    json.getJSONObject("location").getString("country"));

            info += String.format("Temperature: %.1f C%n",
                    json.getJSONObject("current").getDouble("temp_c"));

            info += String.format("Weather conditions: %s%n",
                    json.getJSONObject("current").getJSONObject("condition")
                            .getString("text"));

            info += String.format("Wind speed: %.1f km/h \n",
                    json.getJSONObject("current").getDouble("wind_kph"));
            return info;
        }

        private String parseForecast(JSONObject json) throws JSONException {
            String info = "";
            info += String.format("Weather forecast in %s \n",
                    json.getJSONObject("location").getString("name"));

            info += String.format("Country: %s%n",
                    json.getJSONObject("location").getString("country"));


            JSONArray c = json.getJSONObject("forecast").getJSONArray("forecastday");

            for(int i = 0; i < c.length(); i++) {
                JSONObject obj = c.getJSONObject(i);
                info += String.format("Forecast for %s%n",
                        obj.getString("date"));

                info += String.format("Weather conditions: %s%n",
                        obj.getJSONObject("day").getJSONObject("condition")
                                .getString("text"));

                info += String.format("Average temperature: %.1f C%n",
                        obj.getJSONObject("day").getDouble("avgtemp_c"));

                info += String.format("Max temperature: %.1f C%n",
                        obj.getJSONObject("day").getDouble("maxtemp_c"));

                info += String.format("Min temperature: %.1f C%n",
                        obj.getJSONObject("day").getDouble("mintemp_c"));

                info += String.format("Max wind speed: %.1f km/h \n",
                        obj.getJSONObject("day").getDouble("maxwind_kph"));
            }

//            System.out.println(3);
//            JSONObject forecasts = json.getJSONObject("forecast").getJSONObject("forecastday");
//            System.out.println(1);
//            String dayOfForecast = forecasts.keys().next();
//            System.out.println(2);
//            if(dayOfForecast == null) {
//                info += "No forecast available for this day";
//                return info;
//            }
//            forecasts = forecasts.getJSONObject(dayOfForecast);
//
//
//            info += String.format("Forecast for %s%n",
//                    forecasts.getString("date"));
//
//            info += String.format("Weather conditions: %s%n",
//                    json.getJSONObject("day").getJSONObject("condition")
//                            .getString("text"));
//
//            info += String.format("Average temperature: %.1f C%n",
//                    forecasts.getJSONObject("day").getDouble("avgtemp_c"));
//
//            info += String.format("Max temperature: %.1f C%n",
//                    forecasts.getJSONObject("day").getDouble("maxtemp_c"));
//
//
//            info += String.format("Min temperature: %.1f C%n",
//                    forecasts.getJSONObject("day").getDouble("mintemp_c"));






            return info;
        }
    }
}