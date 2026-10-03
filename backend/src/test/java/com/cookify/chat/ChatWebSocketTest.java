package com.cookify.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Step 11: live delivery over a real WebSocket (STOMP) connection. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatWebSocketTest {

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;

    private RestClient http;
    private String alice;
    private String bob;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE users, conversations RESTART IDENTITY CASCADE");
        http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        alice = signup("alice");
        bob = signup("bob");
        http.post().uri("/api/chat/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + alice)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("username", "bob")).retrieve().toBodilessEntity();
    }

    @Test
    void messageSentOverRestArrivesLiveForBothMembers() throws Exception {
        BlockingQueue<Map<String, Object>> bobInbox = subscribe(connect(bob), "/user/queue/messages");
        BlockingQueue<Map<String, Object>> aliceInbox = subscribe(connect(alice), "/user/queue/messages");

        http.post().uri("/api/chat/conversations/1/messages").header(HttpHeaders.AUTHORIZATION, "Bearer " + alice)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("body", "Hello over WebSocket!"))
                .retrieve().toBodilessEntity();

        Map<String, Object> received = bobInbox.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull().containsEntry("body", "Hello over WebSocket!").containsEntry("conversationId", 1);
        assertThat(aliceInbox.poll(5, TimeUnit.SECONDS)).isNotNull(); // sender's other tabs update too
    }

    @Test
    void messageSentOverStompIsStoredAndDelivered() throws Exception {
        StompSession aliceSession = connect(alice);
        BlockingQueue<Map<String, Object>> bobInbox = subscribe(connect(bob), "/user/queue/messages");

        aliceSession.send("/app/chat.send", Map.of("conversationId", 1, "body", "Sent via STOMP"));

        assertThat(bobInbox.poll(5, TimeUnit.SECONDS)).containsEntry("body", "Sent via STOMP");
        String history = http.get().uri("/api/chat/conversations/1/messages").header(HttpHeaders.AUTHORIZATION, "Bearer " + bob)
                .retrieve().body(String.class);
        assertThat(history).contains("Sent via STOMP");
    }

    @Test
    void typingIndicatorReachesOtherMember() throws Exception {
        StompSession aliceSession = connect(alice);
        BlockingQueue<Map<String, Object>> bobTyping = subscribe(connect(bob), "/user/queue/typing");

        aliceSession.send("/app/chat.typing", Map.of("conversationId", 1));

        assertThat(bobTyping.poll(5, TimeUnit.SECONDS)).containsEntry("username", "alice");
    }

    @Test
    void connectionWithoutValidTokenIsRejected() {
        assertThatThrownBy(() -> connect("not-a-token")).isInstanceOf(ExecutionException.class);
    }

    // ---- Helpers -------------------------------------------------------------------------

    private String signup(String name) {
        Map<?, ?> body = http.post().uri("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", name, "email", name + "@example.com", "password", "Sup3rSecret!", "confirmPassword", "Sup3rSecret!"))
                .retrieve().body(Map.class);
        return (String) body.get("accessToken");
    }

    private StompSession connect(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connectHeaders,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) throws InterruptedException {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((Map<String, Object>) payload);
            }
        });
        Thread.sleep(200); // let the SUBSCRIBE frame reach the broker
        return queue;
    }
}
