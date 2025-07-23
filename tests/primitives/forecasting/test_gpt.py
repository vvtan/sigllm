import importlib
import sys
import types

import numpy as np


def setup_openai(monkeypatch):
    dummy_openai = types.ModuleType('openai')

    class DummyChoice:
        def __init__(self, text):
            self.message = types.SimpleNamespace(content=text)
            self.logprobs = None

    class DummyChat:
        class completions:
            @staticmethod
            def create(**kwargs):
                return types.SimpleNamespace(choices=[DummyChoice('10,11')])

    class DummyClient:
        def __init__(self):
            self.chat = DummyChat()

    dummy_openai.OpenAI = DummyClient
    dummy_openai.Completion = types.SimpleNamespace(
        create=lambda **kwargs: types.SimpleNamespace(
            choices=[types.SimpleNamespace(text='10,11', logprobs=None)]
        )
    )
    monkeypatch.setitem(sys.modules, 'openai', dummy_openai)

    dummy_tiktoken = types.ModuleType('tiktoken')

    class DummyEncoding:
        def encode(self, text):
            return list(range(len(text)))

    dummy_tiktoken.encoding_for_model = lambda name: DummyEncoding()
    monkeypatch.setitem(sys.modules, 'tiktoken', dummy_tiktoken)


def test_forecast(monkeypatch):
    setup_openai(monkeypatch)
    gpt = importlib.import_module('sigllm.primitives.forecasting.gpt')
    importlib.reload(gpt)

    model = gpt.GPT(samples=1)
    X = np.array(['1,2,3'])
    output = model.forecast(X)

    assert output == [['10,11']]
