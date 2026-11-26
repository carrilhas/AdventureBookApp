package adventure_book_app.config;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Serves the Angular app for its own routes. Angular handles URLs like {@code /play/the-prisoner} in the browser,
 * but when one is opened directly or reloaded the server must answer with {@code index.html}.
 * <p>
 * Forwards GET requests to {@code /index.html} unless they're for the API or a file (the last path segment
 * contains a dot, like {@code main-ABC123.js} or {@code favicon.ico}).
 */
@Component
public class SpaForwardingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isAppRoute(exchange)) {
            ServerWebExchange forwarded = exchange.mutate()
                    .request(exchange.getRequest().mutate().path("/index.html").build())
                    .build();
            return chain.filter(forwarded);
        }
        return chain.filter(exchange);
    }

    private static boolean isAppRoute(ServerWebExchange exchange) {
        if (exchange.getRequest().getMethod() != HttpMethod.GET) {
            return false;
        }
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (path.equals("/api") || path.startsWith("/api/")) {
            return false;
        }
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return !lastSegment.contains(".");
    }
}
