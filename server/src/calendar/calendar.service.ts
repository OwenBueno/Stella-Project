import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import {
  buildPaginatedResult,
  clampLimit,
  cursorFilter,
} from '../common/pagination/cursor.util';
import { upsertByClientId } from '../common/mongo/client-id-upsert';
import { mapDoc, mapDocs } from '../database/document.util';
import { CalendarEvent, CalendarEventDocument } from '../database/schemas/calendar-event.schema';
import { CreateEventDto } from './dto/create-event.dto';
import { UpdateEventDto } from './dto/update-event.dto';

@Injectable()
export class CalendarService {
  constructor(
    @InjectModel(CalendarEvent.name)
    private readonly eventModel: Model<CalendarEventDocument>,
  ) {}

  async list(from?: string, to?: string, limit?: string, cursor?: string) {
    const filter: Record<string, unknown> = { deletedAt: null };
    if (from || to) {
      filter.startAt = {};
      if (from) {
        (filter.startAt as Record<string, Date>).$gte = new Date(from);
      }
      if (to) {
        (filter.startAt as Record<string, Date>).$lte = new Date(to);
      }
    }
    const pageLimit = clampLimit(limit);
    if (cursor || limit) {
      const docs = await this.eventModel
        .find(cursorFilter(filter, cursor))
        .sort({ updatedAt: 1, _id: 1 })
        .limit(pageLimit + 1)
        .exec();
      const mapped = mapDocs(docs);
      return buildPaginatedResult(
        mapped,
        pageLimit,
        (i) => i.id as string,
        (i) => new Date(i.updatedAt as string),
      );
    }
    const items = await this.eventModel.find(filter).sort({ startAt: 1 }).exec();
    return {
      items: mapDocs(items),
      nextCursor: null,
      serverTime: new Date().toISOString(),
    };
  }

  async findOne(id: string) {
    const event = await this.eventModel.findById(id).exec();
    if (!event || event.deletedAt) {
      throw new NotFoundException({
        code: 'EVENT_NOT_FOUND',
        message: `Event ${id} not found`,
      });
    }
    return mapDoc(event)!;
  }

  async create(dto: CreateEventDto) {
    const doc = await upsertByClientId(
      this.eventModel,
      dto.id,
      {
        title: dto.title,
        startAt: new Date(dto.startAt),
        endAt: new Date(dto.endAt),
        linkedTaskId: dto.linkedTaskId ?? null,
        recurrenceRuleJson: dto.recurrenceRuleJson ?? null,
        reminderOffsetsJson: dto.reminderOffsetsJson ?? null,
        updatedAt: new Date(dto.updatedAt),
        deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
      },
      new Date(dto.createdAt),
    );
    return mapDoc(doc)!;
  }

  async update(id: string, dto: UpdateEventDto) {
    const existing = await this.eventModel.findById(id).exec();
    if (!existing) {
      throw new NotFoundException({
        code: 'EVENT_NOT_FOUND',
        message: `Event ${id} not found`,
      });
    }
    const doc = await this.eventModel
      .findOneAndUpdate(
        { _id: id },
        {
          ...(dto.title !== undefined && { title: dto.title }),
          ...(dto.startAt !== undefined && { startAt: new Date(dto.startAt) }),
          ...(dto.endAt !== undefined && { endAt: new Date(dto.endAt) }),
          ...(dto.linkedTaskId !== undefined && {
            linkedTaskId: dto.linkedTaskId,
          }),
          ...(dto.recurrenceRuleJson !== undefined && {
            recurrenceRuleJson: dto.recurrenceRuleJson,
          }),
          ...(dto.reminderOffsetsJson !== undefined && {
            reminderOffsetsJson: dto.reminderOffsetsJson,
          }),
          ...(dto.updatedAt !== undefined && {
            updatedAt: new Date(dto.updatedAt),
          }),
          ...(dto.deletedAt !== undefined && {
            deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
          }),
        },
        { new: true },
      )
      .exec();
    return mapDoc(doc)!;
  }

  async remove(id: string): Promise<void> {
    await this.findOne(id);
    await this.eventModel
      .updateOne({ _id: id }, { deletedAt: new Date(), updatedAt: new Date() })
      .exec();
  }
}
