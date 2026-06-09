package com.hamplz.autocomment;

import com.hamplz.autocomment.config.GithubProperties;
import com.hamplz.autocomment.config.HttpClientProperties;
import com.hamplz.autocomment.config.OpenAiProperties;
import com.hamplz.autocomment.config.RetryProperties;
import com.hamplz.autocomment.config.ReviewDeduplicationProperties;
import com.hamplz.autocomment.config.ReviewStatusProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties({
	OpenAiProperties.class,
	GithubProperties.class,
	HttpClientProperties.class,
	RetryProperties.class,
	ReviewDeduplicationProperties.class,
	ReviewStatusProperties.class
})
public class AutocommentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutocommentApplication.class, args);
	}

}
