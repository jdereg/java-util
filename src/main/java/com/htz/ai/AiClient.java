package com.htz.ai;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls to OpenAI / Grok / Gemini / Claude chat APIs, retried with exponential backoff.
 * <p>
 * Responses are streamed (server-sent events), so long generations don't hit a total-request timeout: the call only
 * fails when no response headers arrive within {@link #HEADERS_TIMEOUT}, or the stream goes silent for
 * {@link #IDLE_TIMEOUT}.
 * <p>
 * The response text is returned as-is. On failure the raw response body is returned instead, with
 * {@code - "httpError":<status>} appended for non-200 responses and for errors reported inside the stream,
 * or {@code - "aiError":"no text in response"} when the stream holds no text (e.g. a refusal or safety block) - in that
 * case the non-text events are returned - use {@link #isError(String)} to detect it.
 * Plain Java on purpose. The API key is passed in, or read from the provider's system property / environment
 * variable ({@code OPENAI_API_KEY}, {@code GROK_API_KEY}, {@code GOOGLE_API_KEY}, {@code ANTHROPIC_API_KEY}) by the overloads without it.
 */
public class AiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiClient.class);

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MILLIS = 1000;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Max wait for the response headers (the {@link HttpRequest} timeout only covers the headers). */
    private static final Duration HEADERS_TIMEOUT = Duration.ofMinutes(2);
    /** Max silence between two lines of the stream. */
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(5);

    private AiClient() {
    }

    /**
     * {@link #callAi(String, String, Number, String, int)} with {@link #DEFAULT_MAX_ATTEMPTS}.
     */
    public static String callAi(String model, String prompt, Number temperature, String data)
            throws IOException, InterruptedException {

        return callAi(model, prompt, temperature, data, DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * {@link #callAi(String, String, Number, String, String, int)} with the API key from the provider's system
     * property or environment variable (see {@link AiProvider#apiKey()}).
     *
     * @throws IllegalStateException when the key isn't set
     */
    public static String callAi(String model, String prompt, Number temperature, String data, int maxAttempts)
            throws IOException, InterruptedException {

        return callAi(model, prompt, temperature, data, AiProvider.fromModel(model).apiKey(), maxAttempts);
    }

    /**
     * {@link #callAi(String, String, Number, String, String, int)} with {@link #DEFAULT_MAX_ATTEMPTS}.
     */
    public static String callAi(String model, String prompt, Number temperature, String data, String apiToken)
            throws IOException, InterruptedException {

        return callAi(model, prompt, temperature, data, apiToken, DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * Calls the model, retrying on error results ({@link #isError(String)}) and I/O failures, sleeping 1s, 2s, 4s...
     * between attempts.
     *
     * @param model       model name, the provider is picked by its prefix (see {@link AiProvider})
     * @param prompt      system prompt / instructions
     * @param temperature optional, {@code null} for the provider default
     * @param data        optional user content, {@code null} to send the prompt only
     * @param apiToken    API key of the model's provider (for OpenAI / Grok a {@code Bearer } prefix is optional)
     * @param maxAttempts total attempts, at least 1
     * @return the last result - check it with {@link #isError(String)}
     * @throws IOException when the last attempt failed with an I/O error (including timeouts)
     */
    public static String callAi(String model, String prompt, Number temperature, String data, String apiToken,
                                int maxAttempts) throws IOException, InterruptedException {

        AiProvider provider = AiProvider.fromModel(model);
        long backoff = INITIAL_BACKOFF_MILLIS;
        for (int attempt = 1; ; attempt++) {
            String result;
            try {
                result = callOnce(provider, model, prompt, temperature, data, apiToken);
                if (!isError(result) || attempt >= maxAttempts) {
                    return result;
                }
            } catch (IOException e) {
                if (attempt >= maxAttempts) {
                    throw e;
                }
                result = e.toString();
            }
            LOGGER.warn("AI Call retried - {} - attempt {} - {}", model, attempt, result);
            Thread.sleep(backoff);
            backoff *= 2;
        }
    }

    private static String callOnce(AiProvider provider, String model, String prompt, Number temperature, String data,
                                   String apiToken) throws IOException, InterruptedException {

        switch (provider) {
            case OPENAI:
                return callOpenAi(model, prompt, temperature, data, apiToken);
            case GROK:
                throw new IOException("Grok provider not implemented yet");
            case GEMINI:
                return callGemini(model, prompt, temperature, data, apiToken);
            case CLAUDE:
                return callClaude(model, prompt, temperature, data, apiToken);
            default:
                throw new IllegalArgumentException("model not supported: " + model);
        }
    }

    public static boolean isError(String result) {
        return result.contains("\"error\":") || result.contains("\"httpError\":") || result.contains("\"aiError\":");
    }

    /**
     * OpenAI chat completions format
     */
    private static String callOpenAi(String model, String prompt, Number temperature, String data, String apiToken)
            throws IOException, InterruptedException {

        JSONObject json = new JSONObject();
        json.put("model", model);
        json.put("stream", true);
        JSONArray messages = new JSONArray();
        JSONObject message;
        message = new JSONObject();
        message.put("role", "system");
        message.put("content", prompt);
        messages.put(message);

        if (data != null) {
            message = new JSONObject();
            message.put("role", "user");
            message.put("content", data);
            messages.put(message);
        }

        json.put("messages", messages);

        if (temperature != null) {
            json.put("temperature", temperature);
        }

        HttpRequest request = jsonPost("https://api.openai.com/v1/chat/completions", json)
                .header("Authorization", apiToken.startsWith("Bearer ") ? apiToken : "Bearer " + apiToken)
                .build();

        return stream(request, AiClient::openAiDelta);
    }

    /**
     * Text of a chat completion chunk, {@code null} when it has none (role chunk, finish chunk, usage chunk).
     */
    private static String openAiDelta(JSONObject chunk) {
        JSONArray choices = chunk.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return null;
        }
        JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
        return delta == null ? null : delta.optString("content", null);
    }

    private static String callGemini(String model, String prompt, Number temperature, String data, String apiToken)
            throws IOException, InterruptedException {

        String url = "https://generativelanguage.googleapis.com/v1/models/" + model + ":streamGenerateContent?alt=sse&key="
                + URLEncoder.encode(apiToken, StandardCharsets.UTF_8);

        JSONObject message = new JSONObject();
        String userText = prompt;
        if (data != null) {
            message.put("systemInstruction", new JSONObject()
                    .put("parts", new JSONArray().put(new JSONObject().put("text", prompt))));
            userText = data;
        }

        JSONObject content = new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(new JSONObject().put("text", userText)));
        message.put("contents", new JSONArray().put(content));

        if (temperature != null) {
            message.put("generationConfig", new JSONObject().put("temperature", temperature));
        }

        return stream(jsonPost(url, message).build(), AiClient::geminiDelta);
    }

    /**
     * Joins the text parts of a streamed chunk, skipping thought parts; {@code null} when there are none.
     */
    private static String geminiDelta(JSONObject chunk) {
        JSONArray candidates = chunk.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            return null;
        }
        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
        JSONArray parts = content == null ? null : content.optJSONArray("parts");
        if (parts == null) {
            return null;
        }
        StringBuilder text = null;
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part != null && !part.optBoolean("thought") && part.has("text")) {
                text = text == null ? new StringBuilder() : text;
                text.append(part.optString("text"));
            }
        }
        return text == null ? null : text.toString();
    }

    private static String callClaude(String model, String prompt, Number temperature, String data, String apiToken)
            throws IOException, InterruptedException {

        JSONObject postData = new JSONObject()
                .put("model", model)
                .put("max_tokens", 128000)
                .put("stream", true);

        String userText = prompt;
        if (data != null) {
            postData.put("system", prompt);
            userText = data;
        }

        postData.put("messages", new JSONArray().put(new JSONObject()
                .put("role", "user")
                .put("content", userText)));

        if (temperature != null) {
            postData.put("temperature", temperature);
        }

        HttpRequest request = jsonPost("https://api.anthropic.com/v1/messages", postData)
                .header("anthropic-version", "2023-06-01")
                .header("x-api-key", apiToken)
                .build();

        return stream(request, AiClient::claudeDelta);
    }

    /**
     * Text of a {@code text_delta} event; {@code null} for every other event (thinking / tool_use deltas, ping,
     * message_start / message_delta...).
     */
    private static String claudeDelta(JSONObject event) {
        if (!"content_block_delta".equals(event.optString("type"))) {
            return null;
        }
        JSONObject delta = event.optJSONObject("delta");
        return delta != null && "text_delta".equals(delta.optString("type")) ? delta.optString("text", null) : null;
    }

    private static HttpRequest.Builder jsonPost(String url, JSONObject body) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(HEADERS_TIMEOUT)
                .header("Content-type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
    }

    /**
     * Sends the request and reads the server-sent events, joining the text found by {@code extractText} in each event.
     * Returns the raw body with an error marker when the status isn't 200, an event holds an {@code error}, or no
     * event holds text.
     */
    private static String stream(HttpRequest request, Function<JSONObject, String> extractText)
            throws IOException, InterruptedException {

        LineQueue lines = new LineQueue();
        CompletableFuture<HttpResponse<Void>> future = HTTP_CLIENT.sendAsync(request, info -> {
            lines.status = info.statusCode();
            return HttpResponse.BodySubscribers.fromLineSubscriber(lines);
        });
        // failures before the body starts (connect / headers timeout) never reach the subscriber
        future.whenComplete((response, e) -> {
            if (e != null) {
                lines.onError(e instanceof CompletionException && e.getCause() != null ? e.getCause() : e);
            }
        });

        try {
            String line = lines.next();
            if (lines.status != 200) {
                StringBuilder body = new StringBuilder();
                for (; line != null; line = lines.next()) {
                    body.append(line).append('\n');
                }
                return body.toString().trim() + " - \"httpError\":" + lines.status;
            }

            StringBuilder text = new StringBuilder();
            StringBuilder other = new StringBuilder();  // events without text, returned when there's no text
            StringBuilder eventData = new StringBuilder();
            for (; ; line = lines.next()) {
                if (line != null && !line.isEmpty()) {
                    if (line.startsWith("data:")) {
                        String value = line.substring(5);
                        eventData.append(eventData.length() > 0 ? "\n" : "")
                                .append(value.startsWith(" ") ? value.substring(1) : value);
                    }
                    // "event:", "id:", ":" comment lines - the JSON payload carries everything needed
                    continue;
                }

                // blank line or end of stream - dispatch the event
                String event = eventData.toString();
                eventData.setLength(0);
                if ("[DONE]".equals(event)) {
                    break;
                }
                if (!event.isEmpty()) {
                    String fragment = null;
                    try {
                        JSONObject json = new JSONObject(event);
                        if (json.has("error")) {
                            // e.g. overloaded mid-stream, reported after a 200 status
                            return event + " - \"httpError\":" + lines.status;
                        }
                        fragment = extractText.apply(json);
                    } catch (JSONException e) {
                        // not the expected shape, keep it with the other events
                    }
                    if (fragment != null && !fragment.isEmpty()) {
                        text.append(fragment);
                    } else {
                        other.append(event).append('\n');
                    }
                }
                if (line == null) {
                    break;
                }
            }

            if (text.length() > 0) {
                return text.toString();
            }
            return other.toString().trim() + " - \"aiError\":\"no text in response\"";
        } finally {
            lines.cancel();
            future.cancel(true);
        }
    }

    /**
     * Hands the response lines over to the calling thread, so it can give up when the stream goes silent.
     */
    private static final class LineQueue implements Flow.Subscriber<String> {

        private static final Object END = new Object();

        private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        private volatile Flow.Subscription subscription;
        private volatile int status;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(String line) {
            queue.add(line);
        }

        @Override
        public void onError(Throwable throwable) {
            queue.add(throwable);
        }

        @Override
        public void onComplete() {
            queue.add(END);
        }

        /**
         * @return the next line, {@code null} at the end of the stream
         * @throws HttpTimeoutException when no line arrives within {@link #IDLE_TIMEOUT}
         */
        String next() throws IOException, InterruptedException {
            Object item = queue.poll(IDLE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (item == null) {
                throw new HttpTimeoutException("no data received for " + IDLE_TIMEOUT.toSeconds() + "s");
            }
            if (item == END) {
                return null;
            }
            if (item instanceof IOException) {
                throw (IOException) item;
            }
            if (item instanceof Throwable) {
                throw new IOException((Throwable) item);
            }
            return (String) item;
        }

        void cancel() {
            Flow.Subscription s = subscription;
            if (s != null) {
                s.cancel();
            }
        }
    }
}
