package com.example.sigllm.prompter

import ai.djl.Application
import ai.djl.Model
import ai.djl.huggingface.zoo.HuggingFaceModelZoo
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ModelNotFoundException
import ai.djl.repository.zoo.ZooModel
import ai.djl.training.util.ProgressBar
import ai.djl.translate.TranslateException
import ai.djl.inference.Predictor

/**
 * Groovy implementation of HuggingFace prompter using DJL.
 * This roughly mirrors {@code sigllm.primitives.prompting.huggingface.HF}.
 */
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

    ZooModel<String, String> model

    HFPrompter(Map opts=[:]) throws ModelNotFoundException, IOException {
        opts.each { k, v -> if(this.hasProperty(k)) this."$k" = v }
        Criteria<String, String> criteria = Criteria.builder()
                .setTypes(String.class, String.class)
                .optApplication(Application.NLP.TEXT_GENERATION)
                .optModelZoo(HuggingFaceModelZoo.REF)
                .optModelName(name)
                .optProgress(new ProgressBar())
                .build()
        model = criteria.loadModel()
    }

    List<List<String>> detect(List<String> X) throws TranslateException, IOException {
        int inputLength = X[0].length()
        int maxTokens = (int)(inputLength * anomalousPercent)
        Predictor<String, String> predictor = model.newPredictor()
        List<List<String>> results = []
        X.each { text ->
            String prompt = text + sep
            String prediction = predictor.predict(prompt)
            results << [prediction]
        }
        predictor.close()
        return results
    }
}
