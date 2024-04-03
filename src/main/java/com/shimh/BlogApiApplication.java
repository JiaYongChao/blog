package com.shimh;

import com.shimh.utils.RSAUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class BlogApiApplication {

	public static void main(String[] args) {
		//        生成共私钥
		Map<String, String> rsaKeys = RSAUtils.createRSAKeys();
		String publicKey = rsaKeys.get(RSAUtils.PUBLIC_KEY_NAME);
		System.out.println("publicKey:"+ publicKey);
		String privateKey = rsaKeys.get(RSAUtils.PRIVATE_KEY_NAME);
		System.out.println("privateKey:"+ privateKey);
		SpringApplication app = new SpringApplication(BlogApiApplication.class);
		app.run(args);
	}
}
