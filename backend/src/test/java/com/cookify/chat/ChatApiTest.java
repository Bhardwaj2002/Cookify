package com.cookify.chat;

import com.cookify.support.AuthApi;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.cookify.support.AuthApi.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 11: conversations, messages, unread counts and access rules over REST. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String alice;
    private String bob;
    private String eve;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users, conversations RESTART IDENTITY CASCADE");
        AuthApi api = new AuthApi(mockMvc);
        alice = json(api.signup("alice", "alice@example.com").andReturn(), "$.accessToken");
        bob = json(api.signup("bob", "bob@example.com").andReturn(), "$.accessToken");
        eve = json(api.signup("eve", "eve@example.com").andReturn(), "$.accessToken");
    }

    @Test
    void startingTwiceReturnsSameConversation() throws Exception {
        Integer first = JsonPath.read(start(alice, "bob").andExpect(status().isCreated())
                .andExpect(jsonPath("$.with.username").value("bob"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        start(bob, "ALICE").andExpect(jsonPath("$.id").value(first)).andExpect(jsonPath("$.with.username").value("alice"));
    }

    @Test
    void messagesUnreadCountsAndRead() throws Exception {
        start(alice, "bob");
        send(alice, 1, "Hi Bob! Loved your dal.").andExpect(status().isCreated())
                .andExpect(jsonPath("$.sender.username").value("alice"));
        send(alice, 1, "Any tips for tempering?");

        perform(get("/api/chat/unread"), bob).andExpect(jsonPath("$.unread").value(2));
        perform(get("/api/chat/unread"), alice).andExpect(jsonPath("$.unread").value(0));
        perform(get("/api/chat/conversations"), bob)
                .andExpect(jsonPath("$[0].with.username").value("alice"))
                .andExpect(jsonPath("$[0].unreadCount").value(2))
                .andExpect(jsonPath("$[0].lastMessage.body").value("Any tips for tempering?"));

        perform(get("/api/chat/conversations/1/messages"), bob)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].body").value("Hi Bob! Loved your dal.")); // oldest first

        perform(post("/api/chat/conversations/1/read"), bob).andExpect(status().isNoContent());
        perform(get("/api/chat/unread"), bob).andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void olderMessagesLoadWithCursor() throws Exception {
        start(alice, "bob");
        for (int i = 1; i <= 5; i++) {
            send(alice, 1, "message " + i);
        }
        perform(get("/api/chat/conversations/1/messages").param("size", "2"), bob)
                .andExpect(jsonPath("$[0].body").value("message 4"))
                .andExpect(jsonPath("$[1].body").value("message 5"));
        perform(get("/api/chat/conversations/1/messages").param("size", "2").param("beforeId", "4"), bob)
                .andExpect(jsonPath("$[0].body").value("message 2"))
                .andExpect(jsonPath("$[1].body").value("message 3"));
    }

    @Test
    void outsidersCannotReadOrWrite() throws Exception {
        start(alice, "bob");
        perform(get("/api/chat/conversations/1/messages"), eve).andExpect(status().isNotFound());
        send(eve, 1, "let me in").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/chat/conversations")).andExpect(status().isUnauthorized());
    }

    @Test
    void validationAndModeration() throws Exception {
        start(alice, "alice").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("CANNOT_CHAT_SELF"));
        start(alice, "nobody").andExpect(status().isNotFound());
        start(alice, "bob");
        send(alice, 1, "   ").andExpect(status().isBadRequest());
        send(alice, 1, "you moron")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CONTENT_BLOCKED"));
        perform(get("/api/chat/conversations/1/messages"), bob).andExpect(jsonPath("$.length()").value(0));
    }

    private ResultActions start(String token, String username) throws Exception {
        return perform(post("/api/chat/conversations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\"}"), token);
    }

    private ResultActions send(String token, int conversationId, String body) throws Exception {
        return perform(post("/api/chat/conversations/" + conversationId + "/messages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + body + "\"}"), token);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
