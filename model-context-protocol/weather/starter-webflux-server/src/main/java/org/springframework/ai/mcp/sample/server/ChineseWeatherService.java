package org.springframework.ai.mcp.sample.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ChineseWeatherService {

    private static final String BASE_URL = "https://restapi.amap.com/v3/weather/weatherInfo?city={cityCode}&key={user_key}";

    @Value("${gaode_key}")
    private String gaodeWeatherKey = "";

    private final RestClient restClient;

    public ChineseWeatherService() {
        this.restClient = RestClient.builder().build();
    }


    public record WeatherResponse(
            @JsonProperty("status") String status,
            @JsonProperty("count") String count,
            @JsonProperty("info") String info,
            @JsonProperty("infocode") String infocode,
            @JsonProperty("lives") WeatherDetail[] lives
    ) {
    }

    public record WeatherDetail(
            @JsonProperty("province") String province,
            @JsonProperty("city") String city,
            @JsonProperty("adcode") String adcode,
            @JsonProperty("weather") String weather,
            @JsonProperty("temperature") String temperature,
            @JsonProperty("winddirection") String winddirection,
            @JsonProperty("windpower") String windpower,
            @JsonProperty("humidity") String humidity,
            @JsonProperty("reporttime") String reporttime,
            @JsonProperty("temperature_float") String temperatureFloat,
            @JsonProperty("humidity_float") String humidityFloat
    ) {
    }

    @Tool(description = "根据城市编码查询中国城市的天气")
    public String getWeatherForecastByChineseLocation(String cityCode) {

        var response = restClient.get()
                .uri(BASE_URL, cityCode, gaodeWeatherKey)
                .retrieve()
                .body(WeatherResponse.class);

        if (response != null) {
            WeatherDetail cityWeather = response.lives[0];
            String city = cityWeather.province + cityWeather.city;
            String weather = cityWeather.weather;
            String temperatureFloat = cityWeather.temperatureFloat;

            return city + "的天气: " + weather + ", 温度: " + temperatureFloat + "摄氏度";
        }

        return "query failed!";
    }
}
