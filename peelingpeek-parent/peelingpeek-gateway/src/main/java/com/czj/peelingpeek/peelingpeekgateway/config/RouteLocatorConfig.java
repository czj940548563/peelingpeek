package com.czj.peelingpeek.peelingpeekgateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: clownc
 * @Date: 2019-06-06 10:06
 */
@Configuration
public class RouteLocatorConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                //这里转发的路由可以弄成动态路由的形式来管理，在数据库中存储。
                .route(r->r.path("/api/exampleService/getAccount").uri("http://127.0.0.1:6999/api/mq/getAccount"))
                .build();

    }

}
