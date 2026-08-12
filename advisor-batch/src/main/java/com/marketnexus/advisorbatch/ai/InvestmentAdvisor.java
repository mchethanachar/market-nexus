package com.marketnexus.advisorbatch.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.marketnexus.advisorbatch.model.Article;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvestmentAdvisor {

    private static final String SYSTEM_PROMPT = """
            You are a markets-news analyst assistant covering global equity markets.

            You will be given a batch of recent financial news articles gathered this cycle, \
            potentially spanning multiple countries and regions. Read across all of them \
            together and identify the notable themes, risks, and opportunities they \
            collectively point to for an investor tracking international markets.

            Guidelines:
            - Keep your entire response to 10 sentences or fewer, in plain prose — no headers, \
              no bullet lists, no per-article breakdown.
            - Synthesize across the whole batch — do not just summarize each article in turn.
            - Be specific about which companies/sectors/regions support each point you make.
            - If, and only if, the batch clearly and strongly supports a buy or sell signal on a \
              specific stock or sector, state it plainly and prominently as its own sentence \
              starting with "STRONG BUY:" or "STRONG SELL:", followed by the one or two facts \
              driving it. Do not manufacture a signal the evidence doesn't really support.
            - For everything else, stick to cautious, informational guidance (things worth \
              watching, risks to weigh).
            - The last sentence must be a one-line disclaimer that this is not financial advice.
            """;

    private static final int MAX_CONTENT_CHARS = 3000;

    private final AnthropicClient client = AnthropicOkHttpClient.fromEnv();

    public String synthesize(List<Article> articles) {
        String userPrompt = buildUserPrompt(articles);

        MessageCreateParams params = MessageCreateParams.builder()
                // Model.of(...) is used because this SDK version (2.34.0) predates a named
                // Model.CLAUDE_OPUS_4_8 constant; the model ID string itself is current.
                .model(Model.of("claude-opus-4-8"))
                .maxTokens(4096L)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .system(SYSTEM_PROMPT)
                .addUserMessage(userPrompt)
                .build();

        Message response = client.messages().create(params);

        StringBuilder result = new StringBuilder();
        response.content().stream()
                .flatMap(block -> block.text().stream())
                .forEach(textBlock -> result.append(textBlock.text()));
        return result.toString();
    }

    private String buildUserPrompt(List<Article> articles) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Here are ").append(articles.size())
                .append(" news articles gathered this cycle:\n\n");

        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            String content = article.content() == null ? "" : article.content();
            if (content.length() > MAX_CONTENT_CHARS) {
                content = content.substring(0, MAX_CONTENT_CHARS) + "...";
            }
            prompt.append("Article ").append(i + 1).append(":\n")
                    .append("Source: ").append(article.sourceName()).append("\n")
                    .append("Title: ").append(article.title()).append("\n")
                    .append("Link: ").append(article.link()).append("\n")
                    .append("Content: ").append(content).append("\n\n");
        }

        prompt.append("Based on everything above, give me one aggregated investing takeaway ")
                .append("for this cycle, per the guidelines in your instructions.");
        return prompt.toString();
    }
}
