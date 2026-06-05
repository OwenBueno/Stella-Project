import type { Model } from 'mongoose';

/**
 * Idempotent upsert for client-generated UUID _id (offline-first create may POST twice).
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any -- Stella schemas use string UUID _id
export async function upsertByClientId(
  model: Model<any>,
  id: string,
  setFields: Record<string, unknown>,
  createdAt: Date,
) {
  return model
    .findOneAndUpdate(
      { _id: id },
      {
        $set: setFields,
        $setOnInsert: { createdAt },
      },
      { upsert: true, new: true, setDefaultsOnInsert: true },
    )
    .exec();
}

/**
 * Upsert by natural key (e.g. date); _id is set only on insert.
 */
export async function upsertByNaturalKey(
  model: Model<any>,
  filter: Record<string, unknown>,
  setFields: Record<string, unknown>,
  insertFields: Record<string, unknown>,
) {
  return model
    .findOneAndUpdate(
      filter,
      {
        $set: setFields,
        $setOnInsert: insertFields,
      },
      { upsert: true, new: true, setDefaultsOnInsert: true },
    )
    .exec();
}
