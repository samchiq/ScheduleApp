package com.example.scheduleapp;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Location {

    private static final String BASE_URL = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "ScheduleApp/1.0";

    /**
     * Поиск локации по текстовому запросу (адрес, название места)
     * @param query Строка поиска (например, "Haifa, Israel" или "Central Station Tel Aviv")
     * @return LocationResult с адресом и координатами, или null если не найдено
     */
    public static LocationResult searchLocation(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }

        try {
            String encodedQuery = URLEncoder.encode(query.trim(), "UTF-8");
            String urlString = BASE_URL + "/search?q=" + encodedQuery
                    + "&format=json&limit=1&addressdetails=1";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(10000); // 10 секунд
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8")
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();

            // Парсинг JSON ответа
            JSONArray jsonArray = new JSONArray(response.toString());
            if (jsonArray.length() > 0) {
                JSONObject place = jsonArray.getJSONObject(0);

                String displayName = place.getString("display_name");
                double lat = place.getDouble("lat");
                double lon = place.getDouble("lon");

                // Извлекаем детали адреса если есть
                String city = "";
                String country = "";

                if (place.has("address")) {
                    JSONObject address = place.getJSONObject("address");
                    city = address.optString("city",
                            address.optString("town",
                                    address.optString("village", "")));
                    country = address.optString("country", "");
                }

                return new LocationResult(displayName, lat, lon, city, country);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Обратное геокодирование - получение адреса по координатам
     * @param lat Широта
     * @param lon Долгота
     * @return LocationResult с адресом, или null если не найдено
     */
    public static LocationResult reverseGeocode(double lat, double lon) {
        try {
            String urlString = BASE_URL + "/reverse?lat=" + lat
                    + "&lon=" + lon + "&format=json&addressdetails=1";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8")
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();

            JSONObject json = new JSONObject(response.toString());
            String displayName = json.getString("display_name");

            String city = "";
            String country = "";

            if (json.has("address")) {
                JSONObject address = json.getJSONObject("address");
                city = address.optString("city",
                        address.optString("town",
                                address.optString("village", "")));
                country = address.optString("country", "");
            }

            return new LocationResult(displayName, lat, lon, city, country);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Класс для хранения результата геокодирования
     */
    public static class LocationResult {
        public String fullAddress;
        public double latitude;
        public double longitude;
        public String city;
        public String country;

        public LocationResult(String address, double lat, double lon) {
            this.fullAddress = address;
            this.latitude = lat;
            this.longitude = lon;
            this.city = "";
            this.country = "";
        }

        public LocationResult(String address, double lat, double lon, String city, String country) {
            this.fullAddress = address;
            this.latitude = lat;
            this.longitude = lon;
            this.city = city;
            this.country = country;
        }

        @Override
        public String toString() {
            return fullAddress;
        }
    }
}
