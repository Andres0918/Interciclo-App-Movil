package com.example.demo.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.function.Predicate

@Configuration
class WebConfig: WebMvcConfigurer {

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix(
            "/app",
            Predicate { c: Class<*>? ->
                c!!.getPackage().getName().startsWith("com.example.demo.controller")
            })
    }

}