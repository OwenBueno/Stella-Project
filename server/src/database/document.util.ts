// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function mapDoc(doc: any): Record<string, unknown> | null {
  if (!doc) return null;
  const plain =
    typeof doc.toObject === 'function'
      ? doc.toObject({ virtuals: true })
      : { ...doc };
  const rawId = plain._id ?? plain.id ?? doc._id ?? doc.id;
  const id = rawId != null ? String(rawId) : undefined;
  const { _id: _removedId, id: _removedVirtual, ...rest } = plain;
  void _removedId;
  void _removedVirtual;
  return id !== undefined ? { ...rest, id } : { ...rest };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function mapDocs(docs: any[]): Record<string, unknown>[] {
  return docs.map((d) => mapDoc(d)!);
}
