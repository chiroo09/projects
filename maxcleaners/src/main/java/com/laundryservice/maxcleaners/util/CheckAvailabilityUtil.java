package com.laundryservice.maxcleaners.util;


import java.util.List;
import java.util.Map;
import com.laundryservice.maxcleaners.constant.MaxcleanerConstants;
import com.laundryservice.maxcleaners.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Author Tejesh
 */
@Component
public class CheckAvailabilityUtil {

    private static final Logger logger = LoggerFactory.getLogger(CheckAvailabilityUtil.class);

    @Autowired
    private RestTemplate restTemplate;

    public Response valiadeAddressToServe(String address) throws Exception {

        String url = UriComponentsBuilder.fromHttpUrl(MaxcleanerConstants.GEO_BASEURL)
                .queryParam(MaxcleanerConstants.ADDRESS_KEY, address)
                .queryParam(MaxcleanerConstants.BENCHMARK_KEY, MaxcleanerConstants.BENCHMARK_VAL) // Use the desired benchmark
                .queryParam(MaxcleanerConstants.FORMAT_KEY, MaxcleanerConstants.FORMAT_VAL).toUriString();

        logger.info("valiadeAddressToServe  url::" + url);
        Map<String, Object> res_map = restTemplate.getForObject(url, Map.class);
        logger.info("valiadeAddressToServe rec_data::" + res_map);
        boolean matchStatus = extractAddressMatch((Map<String, Object>) res_map.get(MaxcleanerConstants.RESULT_RES));
        Response response = new Response();
        response.setServiceStatus(matchStatus);
        if (matchStatus) {
            response.setMessage(MaxcleanerConstants.ADDRESS_SUCESSES_MSG);
        } else {
            response.setMessage(MaxcleanerConstants.ADDRESS_ERR_MSG);
        }
        return response;
    }

    public boolean extractAddressMatch(Map<String, Object> res_map) {
        boolean status = false;
        try {
            if (res_map != null && res_map.get(MaxcleanerConstants.ADDRESS_MATCH_RES) != null) {
                List<Map<String, Object>> addMatchLis = (List<Map<String, Object>>) res_map
                        .get(MaxcleanerConstants.ADDRESS_MATCH_RES);
                if (addMatchLis != null && !addMatchLis.isEmpty()) {
                    Map<String, Object> addressMap = addMatchLis.get(0);
                    Map<String, Object> cordinateMap = (Map<String, Object>) addressMap.get(MaxcleanerConstants.COORDINATE_RES);
                    if (cordinateMap != null) {
                        double latitude = cordinateMap.get(MaxcleanerConstants.LAT_RES) != null
                                ? Double.parseDouble(cordinateMap.get(MaxcleanerConstants.LAT_RES).toString())
                                : 0;
                        double longitude = cordinateMap.get(MaxcleanerConstants.LON_RES) != null
                                ? Double.parseDouble(cordinateMap.get(MaxcleanerConstants.LON_RES).toString())
                                : 0;
                        if (latitude != 0 && longitude != 0) {
                            double miles = calculateDistance(latitude, longitude);
                            if (miles >= 0 && miles <= MaxcleanerConstants.MILES) {
                                status = true;
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            logger.error("Exception occur during extractAddressMatch::", e);
        }

        return status;
    }

    private double calculateDistance(double latitude, double logitude) {
        // Haversine formula to calculate distance between two coordinates
        double earthRadius = 3963.0;
        double miles = 0;
        logger.info("calculateDistance latitude::" + latitude);
        logger.info("calculateDistance logitude::" + logitude);
        try {
            // Convert latitude and longitude from degrees to radians
            double storeLatitude = MaxcleanerConstants.stores.get(0).getLatitude();
            double storeLongititude = MaxcleanerConstants.stores.get(0).getLongitude();
            logger.info("calculateDistance storeLatitude::" + storeLatitude);
            logger.info("calculateDistance storeLongititude::" + storeLongititude);
            double lat1Rad = Math.toRadians(storeLatitude);
            double lon1Rad = Math.toRadians(storeLongititude);
            double lat2Rad = Math.toRadians(latitude);
            double lon2Rad = Math.toRadians(logitude);


            logger.info("lat1Rad::" + lat1Rad);
            logger.info("lon1Rad::" + lon1Rad);
            logger.info("lat2Rad::" + lat2Rad);
            logger.info("lon2Rad::" + lon2Rad);
            // Haversine formula
            double deltaLat = lat2Rad - lat1Rad;
            double deltaLon = lon2Rad - lon1Rad;
            double a = Math.pow(Math.sin(deltaLat / 2), 2)
                    + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.pow(Math.sin(deltaLon / 2), 2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            miles = earthRadius * c;
        } catch (Exception e) {
            // TODO: handle exception
        }
        logger.info("calculateDistance miles::" + miles);

        return miles; // Distance in miles
    }
}
