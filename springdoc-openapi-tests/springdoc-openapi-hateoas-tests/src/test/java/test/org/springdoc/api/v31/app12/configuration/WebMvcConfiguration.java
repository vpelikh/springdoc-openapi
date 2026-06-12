package test.org.springdoc.api.v31.app12.configuration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Bean
    JacksonJsonHttpMessageConverter getMappingJacksonHttpMessageConverter() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL).withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(jsonMapper);
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
        return converter;
    }

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(0, new ByteArrayHttpMessageConverter());
	}
}
