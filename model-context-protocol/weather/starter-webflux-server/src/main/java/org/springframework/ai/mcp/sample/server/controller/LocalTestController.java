package org.springframework.ai.mcp.sample.server.controller;

import org.springframework.ai.mcp.sample.server.ChineseWeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class LocalTestController {
    private final ChineseWeatherService weatherService;

    public LocalTestController(ChineseWeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/test")
    public Mono<String> queryWeather(@RequestParam String cityCode) {
        return Mono.just(weatherService.getWeatherForecastByChineseLocation(cityCode));
    }
}
