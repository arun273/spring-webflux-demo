package com.arun.demo.service;

import com.arun.demo.exception.ServerException;
import com.arun.demo.model.User;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;


@Component
@Slf4j
public class HttpWebClient {

    private static final String BASE_URL = "http://localhost:9000";
    private static final String USER_AGENT = "Arun Spring 5 WebClient";

    private final HttpClient httpClient;

    private final WebClient webClient;

    @Autowired
    public HttpWebClient() {
        this.httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(5000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS)));


        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .filter(logRequest())
                .filter(logResposne())
                .build();
    }


    public Mono<User> postUser(User userRequest) {
        return webClient.post()
                .uri("/user")
                .body(Mono.just(userRequest), User.class)
                .retrieve()
                .bodyToMono(User.class);
    }


    public Flux<User> listUsers() {
        return webClient.get()
                .uri("/user")
                .retrieve()

                .onStatus(HttpStatus::is5xxServerError, clientResponse ->
                        Mono.error(new ServerException(HttpStatus.INTERNAL_SERVER_ERROR.toString())))
                .bodyToFlux(User.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                        .filter(throwable -> throwable instanceof ServerException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            throw new ServerException("External Service failed to process after max retries");
                        }));

    }


    public Mono<User> error() {
        return webClient.get()
                .uri("/error")
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse ->
                        Mono.error(new ServerException(HttpStatus.INTERNAL_SERVER_ERROR.toString())))
                .onStatus(HttpStatus::is4xxClientError, clientResponse ->
                        Mono.error(new ServerException(HttpStatus.NOT_FOUND.toString())))
                .bodyToMono(User.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof ServerException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            throw new ServerException("External Service failed to process after max retries");
                        }));

    }


    // Switch expression supported on Jdk 16,17 so on

    public Mono<User> errorWithErrorMap() {
        return webClient.get()
                .uri("/error")
                .retrieve()

                .onStatus(HttpStatus::isError,
                        response -> {
                            if (response.statusCode().is4xxClientError()) {
                                log.error("4xx Error");
                            }
                            if (response.statusCode().is5xxServerError()) {
                                log.error("5xx Error");
                            }
                            return Mono.error(new ServerException(HttpStatus.INTERNAL_SERVER_ERROR.toString()));
                        })

                /*.onStatus(HttpStatus::isError,
                        response -> switch (response.rawStatusCode()) {
                            case 400 -> Mono.error(new ServerException("bad request made"));
                            case 401, 403 -> Mono.error(new Exception("auth error"));
                            case 404 -> Mono.error(new Exception("Maybe not an error?"));
                            case 500 -> Mono.error(new Exception("server error"));
                            default -> Mono.error(new Exception("something went wrong"));
                        })*/
                .bodyToMono(User.class)
                .onErrorMap(Predicate.not(ServerException.class::isInstance), throwable -> {
                    log.error("Failed to send request to service {}", throwable.getMessage(), throwable);
                    return new Exception("other exception");
                });
        // .onErrorMap(Throwable.class, throwable -> new Exception("plain exception"));  this will act on all exceptions

    }


    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers()
                    .forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
            return next.exchange(clientRequest);
        };
    }


    private ExchangeFilterFunction logResposne() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Response Status {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}