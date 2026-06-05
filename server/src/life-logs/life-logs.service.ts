import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import {
  buildPaginatedResult,
  clampLimit,
  cursorFilter,
} from '../common/pagination/cursor.util';
import { mapDocs } from '../database/document.util';
import { LifeLog, LifeLogDocument } from '../database/schemas/life-log.schema';

@Injectable()
export class LifeLogsService {
  constructor(
    @InjectModel(LifeLog.name) private readonly lifeLogModel: Model<LifeLogDocument>,
  ) {}

  async list(since?: string, limit?: string, cursor?: string) {
    const filter: Record<string, unknown> = {};
    if (since) {
      filter.updatedAt = { $gt: new Date(since) };
    }
    const pageLimit = clampLimit(limit);
    const docs = await this.lifeLogModel
      .find(cursorFilter(filter, cursor))
      .sort({ updatedAt: 1, _id: 1 })
      .limit(pageLimit + 1)
      .exec();
    const mapped = mapDocs(docs).map((log) => this.mapForApi(log));
    return buildPaginatedResult(
      mapped,
      pageLimit,
      (i) => i.id as string,
      (i) => new Date(i.updatedAt as string),
    );
  }

  private mapForApi(doc: Record<string, unknown>): Record<string, unknown> {
    const timestamp = doc.timestamp;
    const updatedAt = doc.updatedAt;
    return {
      ...doc,
      timestamp:
        timestamp instanceof Date ? timestamp.toISOString() : timestamp,
      updatedAt:
        updatedAt instanceof Date ? updatedAt.toISOString() : updatedAt,
      payload:
        typeof doc.payload === 'string'
          ? doc.payload
          : JSON.stringify(doc.payload ?? {}),
    };
  }
}
