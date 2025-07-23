package com.example.sigllm.utils

import spock.lang.Specification

class TimeseriesPreprocessingTest extends Specification {
    def "rollingWindowSequences matches python output"() {
        given:
        double[] values = [0.555, 2.345, 1.501, 5.903, 9.116, 3.068, 4.678] as double[]
        int windowSize = 3
        int stepSize = 1

        when:
        def result = TimeseriesPreprocessing.rollingWindowSequences(values, windowSize, stepSize)

        then:
        result.get(0) == [
            [0.555, 2.345, 1.501] as double[],
            [2.345, 1.501, 5.903] as double[],
            [1.501, 5.903, 9.116] as double[],
            [5.903, 9.116, 3.068] as double[],
            [9.116, 3.068, 4.678] as double[],
        ]
        result.get(1) == [0, 1, 2, 3, 4] as int[]
        result.get(2) == windowSize
        result.get(3) == stepSize
    }
}
