package dev.langchain4j.model.azure;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServicesWithJsonSchemaIT;

import java.util.List;

import static dev.langchain4j.model.azure.AzureOpenAiChatModelName.GPT_4_O;
import static dev.langchain4j.model.chat.request.ResponseFormat.JSON;
import static java.util.Arrays.asList;

class AzureOpenAiAiServicesWithJsonSchemaIT extends AiServicesWithJsonSchemaIT {

    @Override
    protected List<ChatLanguageModel> models() {
        return asList(
                AzureOpenAiChatModel.builder()
                        .endpoint(System.getenv("AZURE_OPENAI_ENDPOINT"))
                        .apiKey(System.getenv("AZURE_OPENAI_KEY"))
                        .deploymentName(GPT_4_O.modelName())
                        .tokenizer(new AzureOpenAiTokenizer(GPT_4_O.modelName()))
                        .responseFormat(JSON)
                        .strictJsonSchema(true)
                        .temperature(0.0)
                        .logRequestsAndResponses(true)
                        .build(),
                AzureOpenAiChatModel.builder()
                        .endpoint(System.getenv("AZURE_OPENAI_ENDPOINT"))
                        .apiKey(System.getenv("AZURE_OPENAI_KEY"))
                        .deploymentName(GPT_4_O.modelName())
                        .tokenizer(new AzureOpenAiTokenizer(GPT_4_O.modelName()))
                        .responseFormat(JSON)
                        .strictJsonSchema(false)
                        .temperature(0.0)
                        .logRequestsAndResponses(true)
                        .build()
        );
    }

    @Override
    protected boolean supportsRecursion() {
        return true;
    }
}