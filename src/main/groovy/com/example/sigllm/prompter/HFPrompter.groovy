package com.example.sigllm.prompter

import groovy.json.JsonSlurper

class HFPrompter {
    String name = 'mistralai/Mistral-7B-Instruct-v0.2'
    String sep = ','
    double anomalousPercent = 0.5
    double temp = 1
    double topP = 1
    boolean raw = false
    int samples = 10
    int padding = 0
    boolean restrictTokens = false

    static final Map PROMPTS = new JsonSlurper().parse(
        HFPrompter.class.getResourceAsStream('/com/example/sigllm/prompter/huggingface_messages.json')
    ) as Map

    List<List<String>> detect(List<String> sequences, String normal = null) {
        // Placeholder for calling HuggingFace models via Java bindings
        return []
    }
}
