package com.example.sigllm.prompter

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import okhttp3.*

/**
 * Groovy implementation of the Python GPT prompter.
 *
 * This class mirrors the behaviour of {@code sigllm.primitives.prompting.gpt.GPT}
 * using the OkHttp client to interact with the OpenAI API.
 */
class GPTPrompter {

    String name = 'gpt-3.5-turbo'
    String sep = ','
    double anomalousPercent = 0.5
    double temp = 1
    double topP = 1
    boolean logprobs = false
    Integer topLogprobs = null
    int samples = 10
    Integer seed = null

    Map<String, String> prompts
    OkHttpClient client = new OkHttpClient()
    String apiKey
    String baseUrl = 'https://api.openai.com/v1'

    GPTPrompter(String apiKey, Map opts=[:]) {
        this.apiKey = apiKey
        opts.each { k, v -> if(this.hasProperty(k)) this."$k" = v }
        def promptFile = new File('sigllm/primitives/prompting/gpt_messages.json')
        prompts = new JsonSlurper().parseText(promptFile.text) as Map<String,String>
    }

    /**
     * Call the OpenAI API to detect anomalies.
     * @param X list of input sequences
     * @return list of lists of responses
     */
    List<List<String>> detect(List<String> X) throws IOException {
        int inputLength = X[0].length()
        int maxTokens = (int)(inputLength * anomalousPercent)
        List<List<String>> allResponses = []
        X.each { text ->
            String message = [prompts['user_message'], text, sep].join(' ')
            def payload = [
                model: name,
                messages: [
                    [role: 'system', content: prompts['system_message']],
                    [role: 'user', content: message]
                ],
                max_tokens: maxTokens,
                temperature: temp,
                top_p: topP,
                n: samples
            ]
            if(seed != null) payload['seed'] = seed
            RequestBody body = RequestBody.create(
                JsonOutput.toJson(payload),
                MediaType.get('application/json')
            )
            Request request = new Request.Builder()
                .url("${baseUrl}/chat/completions")
                .addHeader('Authorization', "Bearer ${apiKey}")
                .post(body)
                .build()
            Response response = client.newCall(request).execute()
            def json = new JsonSlurper().parseText(response.body().string())
            List<String> responses = []
            json.choices.each { choice ->
                responses << choice.message.content
            }
            allResponses << responses
        }
        return allResponses
    }
}
