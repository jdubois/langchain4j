package dev.langchain4j.model.github;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import static dev.langchain4j.data.message.UserMessage.userMessage;
import static dev.langchain4j.model.output.FinishReason.STOP;
import static org.assertj.core.api.Assertions.assertThat;

public class GitHubModelsChatModelIT {

    @Test
    void should_generate_answer_and_return_token_usage_and_finish_reason_stop() {

        GitHubModelsChatModel model = GitHubModelsChatModel.builder2()
                .apiKey(System.getenv("GITHUB_TOKEN"))
                .deploymentName("Phi-3.5-mini-instruct")
                .logRequestsAndResponses(true)
                .build();

        UserMessage userMessage = userMessage("hello, how are you?");

        Response<AiMessage> response = model.generate(userMessage);

        assertThat(response.content().text()).isNotBlank();

        TokenUsage tokenUsage = response.tokenUsage();
        assertThat(tokenUsage.inputTokenCount()).isEqualTo(13);
        assertThat(tokenUsage.outputTokenCount()).isGreaterThan(1);
        assertThat(tokenUsage.totalTokenCount()).isGreaterThan(14);

        assertThat(response.finishReason()).isEqualTo(STOP);
    }

}
