import { HttpException, HttpStatus } from '@nestjs/common';

const WINDOW_MS = 120_000;
const MAX_PER_WINDOW = 60;

const buckets = new Map<string, { count: number; resetAt: number }>();

export function assertAiRateLimit(key: string): void {
  const now = Date.now();
  const bucket = buckets.get(key);
  if (!bucket || now >= bucket.resetAt) {
    buckets.set(key, { count: 1, resetAt: now + WINDOW_MS });
    return;
  }
  bucket.count += 1;
  if (bucket.count > MAX_PER_WINDOW) {
    throw new HttpException(
      {
        code: 'RATE_LIMITED',
        message: 'Too many AI requests; retry later',
      },
      HttpStatus.TOO_MANY_REQUESTS,
    );
  }
}
