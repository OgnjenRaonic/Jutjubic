package com.example.demo.service;

import com.example.demo.model.GeoPoint;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeoIpService {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    // hvata: "latitude": 44.81  ili "latitude":"44.81"
    private static final Pattern LAT_PATTERN = Pattern.compile("\"latitude\"\\s*:\\s*\"?(-?\\d+(?:\\.\\d+)?)\"?");
    private static final Pattern LON_PATTERN = Pattern.compile("\"longitude\"\\s*:\\s*\"?(-?\\d+(?:\\.\\d+)?)\"?");

    public GeoPoint approximateFromRequest(HttpServletRequest request) {
        String ip = extractClientIp(request);

        if (ip == null || ip.isBlank() || isLocalOrPrivateIp(ip)) {
            return defaultDevPoint();
        }

        try {
            String url = "https://ipapi.co/" + ip + "/json/";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return defaultDevPoint();
            }

            String body = response.body();

            Double lat = extractNumber(body, LAT_PATTERN);
            Double lon = extractNumber(body, LON_PATTERN);

            if (lat == null || lon == null) {
                return defaultDevPoint();
            }

            return new GeoPoint(lat, lon, "IP");
        } catch (Exception e) {
            return defaultDevPoint();
        }
    }

    private Double extractNumber(String json, Pattern p) {
        Matcher m = p.matcher(json);
        if (!m.find()) return null;
        try { return Double.parseDouble(m.group(1)); }
        catch (Exception e) { return null; }
    }

    private GeoPoint defaultDevPoint() {
        // stavi BG/NS kako ti odgovara
        return new GeoPoint(45.2671, 19.8335, "DEFAULT_DEV");
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();

        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();

        return request.getRemoteAddr();
    }

    private boolean isLocalOrPrivateIp(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress();
        } catch (Exception e) {
            return true;
        }
    }
}
