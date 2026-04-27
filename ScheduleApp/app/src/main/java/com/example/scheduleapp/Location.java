package com.example.scheduleapp;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Provides utility methods for geocoding and reverse geocoding using the Nominatim API.
 * Handles network requests to search for locations and retrieve address details.
 */
public class Location {

    /** Base URL for the Nominatim OpenStreetMap API. */
    private static final String BASE_URL = "https://nominatim.openstreetmap.org";
    /** User agent string required for Nominatim API requests. */
    private static final String USER_AGENT = "ScheduleApp/1.0";

    /**
     * Searches for a location based on a text query.
     * Returns the first matching location result with address and coordinates.
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
  
            JSONArray jsonArray = new JSONArray(response.toString());
            if (jsonArray.length() > 0) {
                JSONObject place = jsonArray.getJSONObject(0);

                String displayName = place.getString("display_name");
                double lat = place.getDouble("lat");
                double lon = place.getDouble("lon");
  
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
     * Retrieves address details for a specific set of geographic coordinates.
     * Uses the reverse geocoding endpoint of the Nominatim API.
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
     * Data class that stores the result of a geocoding or reverse geocoding operation.
     */
    public static class LocationResult {
        /** The full formatted address of the location. */
        public String fullAddress;
        /** The latitude coordinate of the location. */
        public double latitude;
        /** The longitude coordinate of the location. */
        public double longitude;
        /** The city or locality name of the location. */
        public String city;
        /** The country name of the location. */
        public String country;

        /**
         * Initializes a location result with address and coordinates.
         */
        public LocationResult(String address, double lat, double lon) {
            this.fullAddress = address;
            this.latitude = lat;
            this.longitude = lon;
            this.city = "";
            this.country = "";
        }

        /**
         * Initializes a location result with full address details and coordinates.
         */
        public LocationResult(String address, double lat, double lon, String city, String country) {
            this.fullAddress = address;
            this.latitude = lat;
            this.longitude = lon;
            this.city = city;
            this.country = country;
        }

        @Override
        /**
         * Returns the full address string for the location.
         */
        public String toString() {
            return fullAddress;
        }
    }
}
