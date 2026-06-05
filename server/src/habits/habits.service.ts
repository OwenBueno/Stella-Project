import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import {
  buildPaginatedResult,
  clampLimit,
  cursorFilter,
} from '../common/pagination/cursor.util';
import { upsertByClientId, upsertByNaturalKey } from '../common/mongo/client-id-upsert';
import { mapDoc, mapDocs } from '../database/document.util';
import { Habit, HabitDocument } from '../database/schemas/habit.schema';
import { HabitCheckIn, HabitCheckInDocument } from '../database/schemas/habit-check-in.schema';
import { CreateHabitDto } from './dto/create-habit.dto';
import { UpdateHabitDto } from './dto/update-habit.dto';
import { UpsertCheckInDto } from './dto/upsert-check-in.dto';

@Injectable()
export class HabitsService {
  constructor(
    @InjectModel(Habit.name) private readonly habitModel: Model<HabitDocument>,
    @InjectModel(HabitCheckIn.name)
    private readonly checkInModel: Model<HabitCheckInDocument>,
  ) {}

  async list(activeOnly = true) {
    const filter = activeOnly
      ? { deletedAt: null, active: true }
      : { deletedAt: null };
    const items = await this.habitModel.find(filter).sort({ sortOrder: 1 }).exec();
    return {
      items: mapDocs(items),
      nextCursor: null,
      serverTime: new Date().toISOString(),
    };
  }

  async create(dto: CreateHabitDto) {
    const doc = await upsertByClientId(
      this.habitModel,
      dto.id,
      {
        name: dto.name,
        sortOrder: dto.sortOrder,
        active: dto.active ?? true,
        updatedAt: new Date(dto.updatedAt),
        deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
      },
      new Date(dto.createdAt),
    );
    return mapDoc(doc)!;
  }

  async update(id: string, dto: UpdateHabitDto) {
    await this.ensureExists(id);
    const doc = await this.habitModel
      .findOneAndUpdate(
        { _id: id },
        {
          ...(dto.name !== undefined && { name: dto.name }),
          ...(dto.sortOrder !== undefined && { sortOrder: dto.sortOrder }),
          ...(dto.active !== undefined && { active: dto.active }),
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
    await this.ensureExists(id);
    await this.habitModel
      .updateOne({ _id: id }, { deletedAt: new Date(), updatedAt: new Date() })
      .exec();
  }

  async listCheckIns(habitId: string, from?: string, to?: string) {
    await this.ensureExists(habitId);
    return this.listCheckInsGlobal(from, to, habitId);
  }

  async listCheckInsGlobal(
    from?: string,
    to?: string,
    habitId?: string,
    limit?: string,
    cursor?: string,
  ) {
    const filter: Record<string, unknown> = {};
    if (habitId) filter.habitId = habitId;
    if (from || to) {
      filter.date = {};
      if (from) (filter.date as Record<string, string>).$gte = from;
      if (to) (filter.date as Record<string, string>).$lte = to;
    }
    const pageLimit = clampLimit(limit);
    const docs = await this.checkInModel
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

  async upsertCheckIn(habitId: string, dto: UpsertCheckInDto) {
    await this.ensureExists(habitId);
    const completedAt =
      dto.status === 'DONE' && dto.completedAt
        ? new Date(dto.completedAt)
        : dto.status === 'DONE'
          ? new Date(dto.updatedAt)
          : null;
    const doc = await upsertByNaturalKey(
      this.checkInModel,
      { habitId, date: dto.date },
      {
        status: dto.status,
        completedAt,
        updatedAt: new Date(dto.updatedAt),
      },
      {
        _id: dto.id,
        habitId,
        date: dto.date,
      },
    );
    return mapDoc(doc)!;
  }

  private async ensureExists(id: string) {
    const habit = await this.habitModel.findById(id).exec();
    if (!habit || habit.deletedAt) {
      throw new NotFoundException({
        code: 'HABIT_NOT_FOUND',
        message: `Habit ${id} not found`,
      });
    }
  }
}
