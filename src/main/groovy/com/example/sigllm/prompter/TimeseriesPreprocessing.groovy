package com.example.sigllm.prompter

class TimeseriesPreprocessing {
    static Tuple4<List<List<Double>>, int[], Integer, Integer> rollingWindowSequences(List<Double> X, int windowSize=500, int stepSize=100) {
        List<List<Double>> out = []
        List<Integer> firstIdx = []
        int start = 0
        int maxStart = X.size() - windowSize + 1
        while (start < maxStart) {
            int end = start + windowSize
            out << X.subList(start, end)
            firstIdx << start
            start += stepSize
        }
        return new Tuple4(out, firstIdx as int[], windowSize, stepSize)
    }
}
