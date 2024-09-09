package dev.langchain4j.model.github;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import com.azure.core.http.ProxyOptions;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.github.spi.GitHubModelsChatModelBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.spi.ServiceHelper.loadFactories;

/**
 * Represents a GitHub Models language model, that has a chat completion interface, such as OpenAI GPT-4o mini.
 * <p>
 * GitHub Models can be found in the marketplace at "https://github.com/marketplace/models".
 * <p>
 * Mandatory parameters for initialization are:
 * - a GitHub token for authentication and
 * - the model name.
 * <p>
 * As GitHub Models are running on Azure, this uses the LangChain4J Azure OpenAI implementation.
 */
public class GitHubModelsChatModel extends AzureOpenAiChatModel {

    private static final Logger logger = LoggerFactory.getLogger(GitHubModelsChatModel.class);

    public static final String GITHUB_MODELS_INFERENCE_ENDPOINT = "https://models.inference.ai.azure.com";

    public GitHubModelsChatModel(String apiKey,
                                String deploymentName,
                                Tokenizer tokenizer,
                                Integer maxTokens,
                                Double temperature,
                                Double topP,
                                Map<String, Integer> logitBias,
                                String user,
                                Integer n,
                                List<String> stop,
                                Double presencePenalty,
                                Double frequencyPenalty,
                                List<AzureChatExtensionConfiguration> dataSources,
                                AzureChatEnhancementConfiguration enhancements,
                                Long seed,
                                ChatCompletionsResponseFormat responseFormat,
                                Duration timeout,
                                Integer maxRetries,
                                ProxyOptions proxyOptions,
                                boolean logRequestsAndResponses,
                                List<ChatModelListener> listeners,
                                String userAgentSuffix) {

        super(GITHUB_MODELS_INFERENCE_ENDPOINT, "", apiKey, deploymentName, tokenizer, maxTokens, temperature, topP, logitBias, user, n, stop, presencePenalty, frequencyPenalty, dataSources, enhancements, seed, responseFormat, timeout, maxRetries, proxyOptions, logRequestsAndResponses, listeners, userAgentSuffix);
    }

    public static GitHubModelsChatModel.Builder builder2() {
        for (GitHubModelsChatModelBuilderFactory factory : loadFactories(GitHubModelsChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new GitHubModelsChatModel.Builder();
    }

    public static class Builder {

        private String apiKey;
        private String deploymentName;
        private Tokenizer tokenizer;
        private Integer maxTokens;
        private Double temperature;
        private Double topP;
        private Map<String, Integer> logitBias;
        private String user;
        private Integer n;
        private List<String> stop;
        private Double presencePenalty;
        private Double frequencyPenalty;
        List<AzureChatExtensionConfiguration> dataSources;
        AzureChatEnhancementConfiguration enhancements;
        Long seed;
        ChatCompletionsResponseFormat responseFormat;
        private Duration timeout;
        private Integer maxRetries;
        private ProxyOptions proxyOptions;
        private boolean logRequestsAndResponses;
        private OpenAIClient openAIClient;
        private String userAgentSuffix;
        private List<ChatModelListener> listeners;

        /**
         * Sets the GitHub Models API key.
         *
         * @param apiKey The GitHub Models API key.
         * @return builder
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the deployment name in GitHub Models. This is a mandatory parameter.
         * <p>
         * GitHub Models can be found in the marketplace at "https://github.com/marketplace/models".
         *
         * @param deploymentName The Deployment name.
         * @return builder
         */
        public Builder deploymentName(String deploymentName) {
            this.deploymentName = deploymentName;
            return this;
        }

        public Builder tokenizer(Tokenizer tokenizer) {
            this.tokenizer = tokenizer;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
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

        public Builder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.stop = stop;
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

        public Builder dataSources(List<AzureChatExtensionConfiguration> dataSources) {
            this.dataSources = dataSources;
            return this;
        }

        public Builder enhancements(AzureChatEnhancementConfiguration enhancements) {
            this.enhancements = enhancements;
            return this;
        }

        public Builder seed(Long seed) {
            this.seed = seed;
            return this;
        }

        public Builder responseFormat(ChatCompletionsResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
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

        public Builder proxyOptions(ProxyOptions proxyOptions) {
            this.proxyOptions = proxyOptions;
            return this;
        }

        public Builder logRequestsAndResponses(Boolean logRequestsAndResponses) {
            this.logRequestsAndResponses = logRequestsAndResponses;
            return this;
        }

        public Builder userAgentSuffix(String userAgentSuffix) {
            this.userAgentSuffix = userAgentSuffix;
            return this;
        }

        /**
         * Sets the Azure OpenAI client. This is an optional parameter, if you need more flexibility than using the endpoint, serviceVersion, apiKey, deploymentName parameters.
         *
         * @param openAIClient The Azure OpenAI client.
         * @return builder
         */
        public Builder openAIClient(OpenAIClient openAIClient) {
            this.openAIClient = openAIClient;
            return this;
        }

        public Builder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public GitHubModelsChatModel build() {
            return new GitHubModelsChatModel(
                        apiKey,
                        deploymentName,
                        tokenizer,
                        maxTokens,
                        temperature,
                        topP,
                        logitBias,
                        user,
                        n,
                        stop,
                        presencePenalty,
                        frequencyPenalty,
                        dataSources,
                        enhancements,
                        seed,
                        responseFormat,
                        timeout,
                        maxRetries,
                        proxyOptions,
                        logRequestsAndResponses,
                        listeners,
                        userAgentSuffix
                );
        }
    }
}
