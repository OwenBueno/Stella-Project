export function isIncomingNewer(
  incomingUpdatedAt: string | Date,
  storedUpdatedAt: Date | null | undefined,
): boolean {
  if (!storedUpdatedAt) return true;
  const incoming = new Date(incomingUpdatedAt).getTime();
  const stored = storedUpdatedAt.getTime();
  return incoming >= stored;
}
