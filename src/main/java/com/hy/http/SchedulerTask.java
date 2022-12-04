package com.hy.http;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hy.http.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class SchedulerTask {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerTask.class);
    @Value("${push-url}")
    private String pushUrl;
    @Value("${data-url}")
    private String dataUrl;
    @Value("${UserAuthorityCode}")
    private String UserAuthorityCode;
    @Value("${region}")
    private String region;
    @Value("${points:}")
    private String[] points;
    private String sep = "_";

    @Scheduled(fixedDelayString = "${interval}")
    public void transferSchedule() {
        logger.info("starting transfer...");
        Result r = this.getRecentData();

        Gas g = new Gas();
        g.setRegion(region);

        if (r.getData() == null) {
            return;
        }

        for (int i = 0; i < r.getData().size(); i++) {
            DataItem item = r.getData().get(i);

            g.setTs(item.getTimeStamp());
            g.setPoint(region + sep + item.getTagName());
            g.setPname(region + sep + item.getTagName());
            g.setValue(item.getValue());
            g.setUnit(item.getUnits());

            WritterResult result = this.addTaos(g);
            logger.info(result.getMessage());
        }
    }

    public Result getRecentData() {
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(dataUrl)
                .queryParam("UserAuthorityCode", UserAuthorityCode)
                .queryParam("tagsStr", points);
        HttpEntity<JSONObject> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, request, String.class);
        Result result = JSON.parseObject(response.getBody(), Result.class);
        return result;
    }

    private WritterResult addTaos(Gas data) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        RestTemplate restTemplate = new RestTemplate();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = sdf.format(data.getTs());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ts", dateString);
        requestBody.put("point", data.getPoint());
        requestBody.put("pname", data.getPname());
        requestBody.put("unit", data.getUnit());
        requestBody.put("region", data.getRegion());
        requestBody.put("value", data.getValue());

        HttpEntity<Map<String, Object>> r = new HttpEntity<>(requestBody, requestHeaders);

        // 请求服务端添加玩家
        WritterResult result = restTemplate.postForObject(pushUrl, r, WritterResult.class);

        return result;
    }
}
