export const DEFAULT_PAGE_LIMIT = 100;
export const MAX_PAGE_LIMIT = 500;

export interface CursorPayload {
  updatedAt: string;
  id: string;
}

export interface PaginatedListResult<T> {
  items: T[];
  nextCursor: string | null;
  serverTime: string;
}

export function clampLimit(limit?: string | number): number {
  const n =
    typeof limit === 'string'
      ? parseInt(limit, 10)
      : typeof limit === 'number'
        ? limit
        : DEFAULT_PAGE_LIMIT;
  if (!Number.isFinite(n) || n < 1) return DEFAULT_PAGE_LIMIT;
  return Math.min(n, MAX_PAGE_LIMIT);
}

export function encodeCursor(updatedAt: Date, id: string): string {
  const payload: CursorPayload = {
    updatedAt: updatedAt.toISOString(),
    id,
  };
  return Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url');
}

export function decodeCursor(cursor?: string): CursorPayload | null {
  if (!cursor?.trim()) return null;
  try {
    const json = Buffer.from(cursor, 'base64url').toString('utf8');
    const parsed = JSON.parse(json) as CursorPayload;
    if (!parsed.updatedAt || !parsed.id) return null;
    return parsed;
  } catch {
    return null;
  }
}

/** Mongo filter for keyset pagination after cursor (updatedAt asc, _id asc). */
export function cursorFilter(
  baseFilter: Record<string, unknown>,
  cursor?: string,
): Record<string, unknown> {
  const decoded = decodeCursor(cursor);
  if (!decoded) return baseFilter;
  const cursorDate = new Date(decoded.updatedAt);
  return {
    ...baseFilter,
    $or: [
      { updatedAt: { $gt: cursorDate } },
      { updatedAt: cursorDate, _id: { $gt: decoded.id } },
    ],
  };
}

export function buildPaginatedResult<T>(
  items: T[],
  limit: number,
  getId: (item: T) => string,
  getUpdatedAt: (item: T) => Date,
): PaginatedListResult<T> {
  const hasMore = items.length > limit;
  const page = hasMore ? items.slice(0, limit) : items;
  const last = page[page.length - 1];
  const nextCursor =
    hasMore && last
      ? encodeCursor(getUpdatedAt(last), getId(last))
      : null;
  return {
    items: page,
    nextCursor,
    serverTime: new Date().toISOString(),
  };
}
