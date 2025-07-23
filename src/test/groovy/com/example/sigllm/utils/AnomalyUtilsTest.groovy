package com.example.sigllm.utils

import spock.lang.Specification

class AnomalyUtilsTest extends Specification {

    def "test_find_anomalies_in_windows"() {
        given:
        def anomalyList = [
            [[0,3] as int[], [1] as int[], [1,2] as int[]],
            [[0] as int[], [1,4] as int[], [2,3] as int[]],
            [[0,2] as int[], [] as int[], [0,1] as int[]]
        ]

        when:
        def result = AnomalyUtils.findAnomaliesInWindows(anomalyList)

        then:
        result == [[1] as int[], [] as int[], [0] as int[]]
    }

    def "test_merge_anomalous_sequences"() {
        given:
        def anomalies = [[0] as int[], [1,2] as int[], [0,2] as int[], [1,2] as int[], [1] as int[]]
        int[] firstIdx = [0,1,2,3,4] as int[]

        when:
        def result = AnomalyUtils.mergeAnomalousSequences(anomalies, firstIdx, 3, 1)

        then:
        result == [2,4,5] as int[]
    }

    def "test_val2idx"() {
        given:
        def anomalousVal = [
            [[0,3] as int[], [] as int[]],
            [[2] as int[], [4] as int[]]
        ]
        def windows = [[0,1,0,3] as int[], [3,2,6,2] as int[]]

        when:
        def result = AnomalyUtils.val2idx(anomalousVal, windows)

        then:
        result == [
            [[0,2,3] as int[], [] as int[]],
            [[1,3] as int[], [] as int[]]
        ]
    }

    def "test_format_anomalies"() {
        given:
        int[] idxList = [32,545,689,1103,1134] as int[]
        double[] timestamp = (1000..12990).step(10).collect{it as double} as double[]

        when:
        def result = AnomalyUtils.formatAnomalies(idxList, timestamp)

        then:
        result == [
            [1000d,1820d,0],
            [5950d,6950d,0],
            [7390d,8390d,0],
            [11530d,12840d,0]
        ]
    }

    def "test_clean_response_no_anomalies"() {
        expect:
        ['no anomalies','NO ANOMALIES','  no anomalies  ','There are no anomalies in this data','No anomaly detected','  No anomaly  '].every {
            AnomalyUtils.cleanResponse(it) == ''
        }
    }

    def "test_clean_response_with_anomalies"() {
        given:
        def cases = [
            '[1, 2, 3]' : '[1, 2, 3]',
            '  [1, 2, 3]  ' : '[1, 2, 3]',
            'Anomalies found at [1, 2, 3]' : 'anomalies found at [1, 2, 3]',
            'ANOMALIES AT [1, 2, 3]' : 'anomalies at [1, 2, 3]'
        ]
        expect:
        cases.every { input, expected -> AnomalyUtils.cleanResponse(input) == expected }
    }

    def "test_parse_list_response_valid"() {
        given:
        def cases = [
            '[1, 2, 3]' : '1,2,3',
            '  [1, 2, 3]  ' : '1,2,3',
            'Anomalies found at [1, 2, 3]' : '1,2,3',
            '[1,2,3]' : '1,2,3',
            '[1, 2, 3, 4, 5]' : '1,2,3,4,5'
        ]
        expect:
        cases.every { input, expected -> AnomalyUtils.parseListResponse(input) == expected }
    }

    def "test_parse_list_response_invalid"() {
        expect:
        ['no anomalies','[]','[ ]','text with [no numbers]','text with [letters, and, symbols]','   '].every {
            AnomalyUtils.parseListResponse(it) == ''
        }
    }

    def "test_parse_list_response_edge"() {
        given:
        def cases = [
            '[1,2,3,]' : '1,2,3',
            '[1,,2,3]' : '1,2,3',
            '[1, 2, 3], [5]' : '1,2,3'
        ]
        expect:
        cases.every { input, expected -> AnomalyUtils.parseListResponse(input) == expected }
    }

    def "test_parse_interval_response_valid"() {
        given:
        def cases = [
            '[[1, 3]]' : [1,2,3],
            '  [[1, 3]]  ' : [1,2,3],
            'Anomalies found at [[1, 3]]' : [1,2,3],
            '[[1, 3], [5, 7]]' : [1,2,3,5,6,7],
            '[[1, 3], [5, 7], [8, 9]]' : [1,2,3,5,6,7,8,9],
            '[[1, 3], [4, 6],]' : [1,2,3,4,5,6],
            '[[1, 2], [3]]' : [1,2],
            '[[1,,3]]' : [1,2,3],
            '[[0, 10]]' : (0..10).toList()
        ]
        expect:
        cases.every { input, expected -> AnomalyUtils.parseIntervalResponse(input) == expected }
    }

    def "test_parse_interval_response_invalid"() {
        expect:
        ['[]','[[]]','text with [no numbers]','[[1]]','[[1, 2, 3]]'].every {
            AnomalyUtils.parseIntervalResponse(it) == []
        }
    }

    def "test_parse_interval_response_multiple"() {
        given:
        def cases = [
            'Found [[1, 3]] and [[5, 7]]' : [1,2,3,5,6,7],
            '[[1, 2]] in first part and [[3, 4]] in second' : [1,2,3,4],
            'Multiple intervals: [[1, 3]], [[4, 6]], [[7, 9]]' : [1,2,3,4,5,6,7,8,9],
            '[[1, 2]] and [[1, 2]] and [[1, 2]]' : [1,2,1,2,1,2]
        ]
        expect:
        cases.every { input, expected -> AnomalyUtils.parseIntervalResponse(input) == expected }
    }

    def "test_parse_anomaly_response"() {
        expect:
        AnomalyUtils.parseAnomalyResponse([["Answer: no anomalies"], ["Answer: no anomaly"], ["no anomaly, with extra"]]) == [[''], [''], ['']]
        AnomalyUtils.parseAnomalyResponse([["Answer: [123]"], ["Answer: [456]", "answer: [789]"]]) == [['123'], ['456','789']]
        AnomalyUtils.parseAnomalyResponse([["Answer: [123, 456, 789]"], ["Answer: [111, 222, 333]"]]) == [['123,456,789'], ['111,222,333']]
        AnomalyUtils.parseAnomalyResponse([["Answer: no anomalies", "Answer: [123, 456]"], ["Answer: [789]", "no anomaly"]]) == [['', '123,456'], ['789', '']]
        AnomalyUtils.parseAnomalyResponse([
            ["Answer: [123, 456]", "Answer: [ 789 , 101 ]"],
            ["Answer: [1,2,3]", "Answer: [ 4 , 5 , 6 ]"]
        ]) == [['123,456','789,101'], ['1,2,3','4,5,6']]
        AnomalyUtils.parseAnomalyResponse([[""], ["Answer: no anomalies"], ["answer"], ["no anomly"]]) == [[''], [''], [''], ['']]
        AnomalyUtils.parseAnomalyResponse([["Answer: invalid format"], ["Answer: [123, abc]"]]) == [[''], ['']]
    }
}
