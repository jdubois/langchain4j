package dev.langchain4j.model.openaiofficial;

import static dev.langchain4j.model.chat.request.ToolChoice.REQUIRED;
import static dev.langchain4j.model.openaiofficial.InternalOpenAiOfficialHelper.convertHandler;
import static dev.langchain4j.model.openaiofficial.InternalOpenAiOfficialHelper.finishReasonFrom;
import static dev.langchain4j.model.openaiofficial.InternalOpenAiOfficialHelper.toOpenAiChatCompletionCreateParams;
import static dev.langchain4j.model.openaiofficial.InternalOpenAiOfficialHelper.tokenUsageFrom;
import static dev.langchain4j.spi.ServiceHelper.loadFactories;

import com.openai.azure.AzureOpenAIServiceVersion;
import com.openai.core.http.StreamResponse;
import com.openai.credential.Credential;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionStreamOptions;
import com.openai.models.ChatModel;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.exception.UnsupportedFeatureException;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openaiofficial.spi.OpenAiOfficialStreamingChatModelBuilderFactory;
import java.net.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenAiOfficialStreamingChatModel extends OpenAiOfficialBaseChatModel
        implements StreamingChatLanguageModel {

    public OpenAiOfficialStreamingChatModel(Builder builder) {

        init(
                builder.baseUrl,
                builder.apiKey,
                builder.azureApiKey,
                builder.credential,
                builder.azureDeploymentName,
                builder.azureOpenAIServiceVersion,
                builder.organizationId,
                builder.defaultRequestParameters,
                builder.modelName,
                builder.temperature,
                builder.topP,
                builder.stop,
                builder.maxCompletionTokens,
                builder.presencePenalty,
                builder.frequencyPenalty,
                builder.logitBias,
                builder.responseFormat,
                builder.strictJsonSchema,
                builder.seed,
                builder.user,
                builder.strictTools,
                builder.parallelToolCalls,
                builder.store,
                builder.metadata,
                builder.serviceTier,
                builder.timeout,
                builder.maxRetries,
                builder.proxy,
                builder.tokenizer,
                builder.customHeaders,
                builder.listeners,
                builder.capabilities);
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        ChatRequest chatRequest = ChatRequest.builder().messages(messages).build();
        chat(chatRequest, convertHandler(handler));
    }

    @Override
    public void generate(
            List<ChatMessage> messages,
            List<ToolSpecification> toolSpecifications,
            StreamingResponseHandler<AiMessage> handler) {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder()
                        .toolSpecifications(toolSpecifications)
                        .build())
                .build();
        chat(chatRequest, convertHandler(handler));
    }

    @Override
    public void generate(
            List<ChatMessage> messages,
            ToolSpecification toolSpecification,
            StreamingResponseHandler<AiMessage> handler) {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder()
                        .toolSpecifications(toolSpecification)
                        .toolChoice(REQUIRED)
                        .build())
                .build();
        chat(chatRequest, convertHandler(handler));
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {

        OpenAiOfficialChatRequestParameters parameters = (OpenAiOfficialChatRequestParameters) chatRequest.parameters();
        InternalOpenAiOfficialHelper.validate(parameters);

        if (this.useAzure) {
            if (!parameters.modelName().equals(this.azureModelName)) {
                throw new UnsupportedFeatureException(
                        "On Azure OpenAI, it is not supported to change the modelName, as it's part of the deployment URL");
            }
        }

        ChatCompletionCreateParams chatCompletionCreateParams = toOpenAiChatCompletionCreateParams(
                        chatRequest, parameters, strictTools, strictJsonSchema)
                .streamOptions(
                        ChatCompletionStreamOptions.builder().includeUsage(true).build())
                .build();

        try (StreamResponse<ChatCompletionChunk> response =
                client.chat().completions().createStreaming(chatCompletionCreateParams)) {

            OpenAiOfficialChatResponseMetadata.Builder responseMetadataBuilder =
                    OpenAiOfficialChatResponseMetadata.builder();
            StringBuffer text = new StringBuffer();
            List<ToolExecutionRequest> toolExecutionRequests = new ArrayList<>();

            response.stream().forEach(chatCompletionChunk -> {
                responseMetadataBuilder.id(chatCompletionChunk.id());
                responseMetadataBuilder.modelName(chatCompletionChunk.model());
                if (chatCompletionChunk.usage().isPresent()) {
                    responseMetadataBuilder.tokenUsage(
                            tokenUsageFrom(chatCompletionChunk.usage().get()));
                }
                responseMetadataBuilder.created(chatCompletionChunk.created());
                responseMetadataBuilder.serviceTier(
                        chatCompletionChunk.serviceTier().isPresent()
                                ? chatCompletionChunk.serviceTier().get().toString()
                                : null);
                responseMetadataBuilder.systemFingerprint(
                        chatCompletionChunk.systemFingerprint().isPresent()
                                ? chatCompletionChunk.systemFingerprint().get()
                                : null);
                chatCompletionChunk.choices().forEach(choice -> {
                    if (choice.delta().content().isPresent()
                            && !choice.delta().content().get().isEmpty()) {
                        text.append(choice.delta().content().get());
                        handler.onPartialResponse(choice.delta().content().get());
                    }
                    if (choice.delta().toolCalls().isPresent()) {
                        choice.delta().toolCalls().get().forEach(toolCall -> {
                            if (toolCall.function().isPresent()) {
                                final ChatCompletionChunk.Choice.Delta.ToolCall.Function function =
                                        toolCall.function().get();
                                final String functionId;
                                final String functionName;
                                final String functionArguments;
                                if (toolCall.id().isPresent()) {
                                    functionId = toolCall.id().get();
                                } else {
                                    functionId = "";
                                }
                                if (function.name().isPresent()) {
                                    functionName = function.name().get();
                                } else {
                                    functionName = "";
                                }
                                if (function.arguments().isPresent()) {
                                    functionArguments = function.arguments().get();
                                } else {
                                    functionArguments = "";
                                }
                                if (!functionId.isEmpty()) {
                                    // A new function is called
                                    toolExecutionRequests.add(ToolExecutionRequest.builder()
                                            .id(functionId)
                                            .name(functionName)
                                            .arguments(functionArguments)
                                            .build());
                                } else {
                                    // This chunk is part of the previous function call
                                    ToolExecutionRequest lastToolExecutionRequest =
                                            toolExecutionRequests.get(toolExecutionRequests.size() - 1);
                                    toolExecutionRequests.remove(lastToolExecutionRequest);
                                    ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                                            .id(functionId)
                                            .name(lastToolExecutionRequest.name() + functionName)
                                            .arguments(lastToolExecutionRequest.arguments() + functionArguments)
                                            .build();
                                    toolExecutionRequests.add(toolExecutionRequest);
                                }
                            }
                        });
                    }
                    if (choice.finishReason().isPresent()) {
                        responseMetadataBuilder.finishReason(
                                finishReasonFrom(choice.finishReason().get()));
                    }
                });
            });

            AiMessage aiMessage;
            if (text.toString().isEmpty() && !toolExecutionRequests.isEmpty()) {
                aiMessage = AiMessage.from(toolExecutionRequests);
            } else if (!text.toString().isEmpty() && toolExecutionRequests.isEmpty()) {
                aiMessage = AiMessage.from(text.toString());
            } else if (!text.toString().isEmpty()) {
                aiMessage = AiMessage.from(text.toString(), toolExecutionRequests);
            } else {
                aiMessage = AiMessage.from();
            }

            ChatResponse chatResponse = ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .metadata(responseMetadataBuilder.build())
                    .build();

            handler.onCompleteResponse(chatResponse);
        } catch (Exception e) {
            handler.onError(e);
        }
    }

    public static Builder builder() {
        for (OpenAiOfficialStreamingChatModelBuilderFactory factory :
                loadFactories(OpenAiOfficialStreamingChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new Builder();
    }

    public static class Builder {

        private String baseUrl;
        private String apiKey;
        private String azureApiKey;
        private Credential credential;
        private String azureDeploymentName;
        private AzureOpenAIServiceVersion azureOpenAIServiceVersion;
        private String organizationId;

        private ChatRequestParameters defaultRequestParameters;
        private String modelName;
        private Double temperature;
        private Double topP;
        private List<String> stop;
        private Integer maxCompletionTokens;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private String responseFormat;
        private Boolean strictJsonSchema;
        private Integer seed;
        private String user;
        private Boolean strictTools;
        private Boolean parallelToolCalls;
        private Boolean store;
        private Map<String, String> metadata;
        private String serviceTier;

        private Duration timeout;
        private Integer maxRetries;
        private Proxy proxy;
        private Tokenizer tokenizer;
        private Map<String, String> customHeaders;
        private List<ChatModelListener> listeners;
        private Set<Capability> capabilities;

        public Builder() {
            // This is public so it can be extended
        }

        /**
         * Sets default common {@link ChatRequestParameters} or OpenAI-specific {@link OpenAiOfficialChatRequestParameters}.
         * <br>
         * When a parameter is set via an individual builder method (e.g., {@link #modelName(String)}),
         * its value takes precedence over the same parameter set via {@link ChatRequestParameters}.
         */
        public Builder defaultRequestParameters(ChatRequestParameters parameters) {
            this.defaultRequestParameters = parameters;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder modelName(ChatModel modelName) {
            this.modelName = modelName.toString();
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder azureApiKey(String azureApiKey) {
            this.azureApiKey = azureApiKey;
            return this;
        }

        public Builder credential(Credential credential) {
            this.credential = credential;
            return this;
        }

        public Builder azureDeploymentName(String azureDeploymentName) {
            this.azureDeploymentName = azureDeploymentName;
            return this;
        }

        public Builder azureOpenAIServiceVersion(AzureOpenAIServiceVersion azureOpenAIServiceVersion) {
            this.azureOpenAIServiceVersion = azureOpenAIServiceVersion;
            return this;
        }

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public Builder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder strictJsonSchema(Boolean strictJsonSchema) {
            this.strictJsonSchema = strictJsonSchema;
            return this;
        }

        public Builder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder strictTools(Boolean strictTools) {
            this.strictTools = strictTools;
            return this;
        }

        public Builder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public Builder store(Boolean store) {
            this.store = store;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder serviceTier(String serviceTier) {
            this.serviceTier = serviceTier;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder tokenizer(Tokenizer tokenizer) {
            this.tokenizer = tokenizer;
            return this;
        }

        public Builder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public Builder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public Builder supportedCapabilities(Set<Capability> capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public OpenAiOfficialStreamingChatModel build() {
            if (azureApiKey != null || credential != null) {
                // Using Azure OpenAI
                if (this.defaultRequestParameters != null && this.defaultRequestParameters.modelName() != null) {
                    if (!this.defaultRequestParameters.modelName().equals(this.modelName)) {
                        throw new UnsupportedFeatureException(
                                "On Azure OpenAI, it is not supported to change the modelName, as it's part of the deployment URL");
                    }
                }
            }
            return new OpenAiOfficialStreamingChatModel(this);
        }
    }
}
