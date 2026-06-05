import {
  DEFAULT_OPENROUTER_MODEL_FALLBACK,
  OpenRouterNotConfiguredError,
  resolveOpenRouterModels,
} from './openrouter-models';

function assert(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function testDefaultFallbackWhenUnset(): void {
  const result = resolveOpenRouterModels({
    OPENROUTER_MODEL: 'anthropic/claude-3.5-sonnet',
  });
  assert(result.primary === 'anthropic/claude-3.5-sonnet', 'primary mismatch');
  assert(
    result.models.length === 2 &&
      result.models[0] === 'anthropic/claude-3.5-sonnet' &&
      result.models[1] === DEFAULT_OPENROUTER_MODEL_FALLBACK,
    'expected primary + default fallback',
  );
}

function testExplicitFallback(): void {
  const result = resolveOpenRouterModels({
    OPENROUTER_MODEL: 'anthropic/claude-3.5-sonnet',
    OPENROUTER_MODEL_FALLBACK: 'openai/gpt-4o-mini',
  });
  assert(
    result.models.length === 2 &&
      result.models[0] === 'anthropic/claude-3.5-sonnet' &&
      result.models[1] === 'openai/gpt-4o-mini',
    'expected explicit fallback',
  );
}

function testDedupeWhenFallbackEqualsPrimary(): void {
  const result = resolveOpenRouterModels({
    OPENROUTER_MODEL: 'deepseek/deepseek-v4-flash',
    OPENROUTER_MODEL_FALLBACK: 'deepseek/deepseek-v4-flash',
  });
  assert(result.models.length === 1, 'expected single model when deduped');
  assert(result.models[0] === 'deepseek/deepseek-v4-flash', 'primary mismatch');
}

function testMissingPrimary(): void {
  let threw = false;
  try {
    resolveOpenRouterModels({});
  } catch (err) {
    threw = err instanceof OpenRouterNotConfiguredError;
  }
  assert(threw, 'expected OpenRouterNotConfiguredError when primary missing');
}

function run(): void {
  testDefaultFallbackWhenUnset();
  testExplicitFallback();
  testDedupeWhenFallbackEqualsPrimary();
  testMissingPrimary();
  console.log('openrouter-models.spec.ts: all tests passed');
}

run();
