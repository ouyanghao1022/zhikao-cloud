package com.zhikao.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PkWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> matchQueue = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> msg = mapper.readValue(message.getPayload(), Map.class);
        String type = (String) msg.get("type");

        switch (type) {
            case "match": handleMatch(session, msg); break;
            case "answer": handleAnswer(session, msg); break;
            case "ready": handleReady(session); break;
        }
    }

    private void handleMatch(WebSocketSession session, Map<String, Object> msg) throws IOException {
        Long userId = Long.valueOf(msg.get("userId").toString());
        matchQueue.put(session.getId(), userId.toString());

        if (matchQueue.size() >= 2) {
            List<String> matched = new ArrayList<>(matchQueue.keySet());
            String p1Id = matched.get(0);
            String p2Id = matched.get(1);
            matchQueue.remove(p1Id);
            matchQueue.remove(p2Id);

            Map<String, Object> start = new HashMap<>();
            start.put("type", "match_found");
            start.put("opponent", matchQueue.getOrDefault(p2Id, "unknown"));

            sendJson(p1Id, start);
            sendJson(p2Id, start);
        }
    }

    private void handleAnswer(WebSocketSession session, Map<String, Object> msg) throws IOException {
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "opponent_answer");
        broadcast.put("questionId", msg.get("questionId"));
        broadcast.put("userId", msg.get("userId"));
        sendToAll(broadcast);
    }

    private void handleReady(WebSocketSession session) throws IOException {
        Map<String, Object> ready = new HashMap<>();
        ready.put("type", "opponent_ready");
        ready.put("userId", session.getId());
        sendToAll(ready);
    }

    private void sendJson(String sessionId, Map<String, Object> data) throws IOException {
        WebSocketSession s = sessions.get(sessionId);
        if (s != null && s.isOpen()) {
            s.sendMessage(new TextMessage(mapper.writeValueAsString(data)));
        }
    }

    private void sendToAll(Map<String, Object> data) throws IOException {
        String json = mapper.writeValueAsString(data);
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) s.sendMessage(new TextMessage(json));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        matchQueue.remove(session.getId());
    }
}
