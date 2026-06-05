export const DEFAULT_OPENROUTER_MODEL_FALLBACK = 'deepseek/deepseek-v4-flash';

export interface OpenRouterModelConfig {
  primary: string;
  models: string[];
}

export class OpenRouterNotConfiguredError extends Error {
  constructor() {
    super('Set OPENROUTER_API_KEY and OPENROUTER_MODEL on the server');
    this.name = 'OpenRouterNotConfiguredError';
  }
}

export function resolveOpenRouterModels(env: NodeJS.ProcessEnv = process.env): OpenRouterModelConfig {
  const primary = env.OPENROUTER_MODEL?.trim();
  if (!primary) {
    throw new OpenRouterNotConfiguredError();
  }

  const fallbackRaw = env.OPENROUTER_MODEL_FALLBACK?.trim();
  const fallback = fallbackRaw || DEFAULT_OPENROUTER_MODEL_FALLBACK;

  if (fallback === primary) {
    return { primary, models: [primary] };
  }

  return { primary, models: [primary, fallback] };
}
