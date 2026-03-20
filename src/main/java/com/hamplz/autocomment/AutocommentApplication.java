package com.hamplz.autocomment;

import com.hamplz.autocomment.config.GithubProperties;
import com.hamplz.autocomment.config.OpenAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
	OpenAiProperties.class,
	GithubProperties.class
})
public class AutocommentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutocommentApplication.class, args);
	}

}
