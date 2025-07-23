package com.example.sigllm.prompter

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import okhttp3.*

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

    OkHttpClient client = new OkHttpClient()
    static final Map PROMPTS = new JsonSlurper().parse(
        GPTPrompter.class.getResourceAsStream('/com/example/sigllm/prompter/gpt_messages.json')
    ) as Map

    List<List<String>> detect(List<String> sequences) {
        List<List<String>> allResponses = []
        sequences.each { text ->
            int maxTokens = (int)(text.length() * anomalousPercent)
            def body = [
                model: name,
                messages: [
                    [role: 'system', content: PROMPTS['system_message']],
                    [role: 'user', content: "${PROMPTS['user_message']} ${text} ${sep}".toString()]
                ],
                max_tokens: maxTokens,
                temperature: temp,
                top_p: topP,
                n: samples,
                seed: seed
            ]
            Request request = new Request.Builder()
                .url('https://api.openai.com/v1/chat/completions')
                .post(RequestBody.create(JsonOutput.toJson(body), MediaType.parse('application/json')))
                .addHeader('Authorization', "Bearer ${System.getenv('OPENAI_API_KEY')}")
                .build()
            Response response = client.newCall(request).execute()
            def json = new JsonSlurper().parseText(response.body().string())
            List<String> choices = json.choices.collect { it.message.content }
            allResponses.add(choices)
        }
        return allResponses
    }
}
