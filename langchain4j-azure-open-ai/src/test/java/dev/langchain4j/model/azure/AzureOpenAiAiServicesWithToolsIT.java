package dev.langchain4j.model.azure;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServicesWithNewToolsIT;
import org.junit.jupiter.api.AfterEach;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

class AzureOpenAiAiServicesWithToolsIT extends AiServicesWithNewToolsIT {

    @Override
    protected List<ChatLanguageModel> models() {
        return asList(
                AzureOpenAiChatModel.builder()
                        .endpoint(System.getenv("AZURE_OPENAI_ENDPOINT"))
                        .apiKey(System.getenv("AZURE_OPENAI_KEY"))
                        .deploymentName("gpt-4o-mini")
                        .temperature(0.0)
                        .logRequestsAndResponses(true)
                        .build(),
                AzureOpenAiChatModel.builder()
                        .endpoint(System.getenv("AZURE_OPENAI_ENDPOINT"))
                        .apiKey(System.getenv("AZURE_OPENAI_KEY"))
                        .deploymentName("gpt-4o-mini")
                        .strictTools(true)
                        .temperature(0.0)
                        .logRequestsAndResponses(true)
                        .build()
        );
    }

    @AfterEach
    void afterEach() throws InterruptedException {
        String ciDelaySeconds = System.getenv("CI_DELAY_SECONDS_AZURE_OPENAI");
        if (ciDelaySeconds != null) {
            Thread.sleep(Integer.parseInt(ciDelaySeconds) * 1000L);
        }
    }
}
