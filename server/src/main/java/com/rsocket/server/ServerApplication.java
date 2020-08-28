package com.rsocket.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SpringBootApplication
public class ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

}

@Configuration
class SecurityConfiguration {
	@Bean
	PayloadSocketAcceptorInterceptor authorization (RSocketSecurity security) {
		return security
				.authorizePayload(ap -> ap.anyExchange().authenticated())
				.simpleAuthentication(Customizer.withDefaults())
				.build();
	}

	@Bean
	MapReactiveUserDetailsService authentication () {
		return new MapReactiveUserDetailsService(User.withDefaultPasswordEncoder().username("jlong").password("pw").roles("USER").build());
	}

	@Bean
	RSocketMessageHandler messageHandler (RSocketStrategies strategies) {
		var rmh = new RSocketMessageHandler();
		rmh.getArgumentResolverConfigurer().addCustomResolver(new AuthenticationPrincipalArgumentResolver());
		rmh.setRSocketStrategies(strategies);
		return rmh;
	}
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse {
	private String message;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest {
	private String name;
}



@Controller
class GreetingController {
	@MessageMapping("greetings")
	Flux<GreetingResponse> greet(RSocketRequester clientRSocketConnection,
								 @AuthenticationPrincipal Mono<User> user) {
		return user.map(User::getUsername)
				.map(GreetingRequest::new)
				.flatMapMany(gr -> this.greet(clientRSocketConnection, gr));

	}


	private Flux<GreetingResponse> greet(RSocketRequester clientRSocketConnection,
								 GreetingRequest request) {

		var in = clientRSocketConnection
				.route("health")
				.retrieveFlux(ClientHealthState.class)
				.filter(chs -> !chs.isHealthy());

		var out = Flux
				.fromStream(Stream.generate(() -> new GreetingResponse("ni hao " + request.getName() + " @ " + Instant.now() + "!")))
				.take(100)
				.delayElements(Duration.ofSeconds(1));
		return out.takeUntilOther(in);

	}
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ClientHealthState {
	private boolean healthy;
}
