import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
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

  async list(from?: string, to?: string) {
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
    const items = await this.eventModel.find(filter).sort({ startAt: 1 }).exec();
    return { items: mapDocs(items) };
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
    const doc = await this.eventModel.create({
      _id: dto.id,
      title: dto.title,
      startAt: new Date(dto.startAt),
      endAt: new Date(dto.endAt),
      linkedTaskId: dto.linkedTaskId ?? null,
      createdAt: new Date(dto.createdAt),
      updatedAt: new Date(dto.updatedAt),
      deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
    });
    return mapDoc(doc)!;
  }

  async update(id: string, dto: UpdateEventDto) {
    await this.findOne(id);
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
