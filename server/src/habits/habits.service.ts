import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
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
    return { items: mapDocs(items) };
  }

  async create(dto: CreateHabitDto) {
    const doc = await this.habitModel.create({
      _id: dto.id,
      name: dto.name,
      sortOrder: dto.sortOrder,
      active: dto.active ?? true,
      createdAt: new Date(dto.createdAt),
      updatedAt: new Date(dto.updatedAt),
      deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
    });
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
    const filter: Record<string, unknown> = { habitId };
    if (from || to) {
      filter.date = {};
      if (from) (filter.date as Record<string, string>).$gte = from;
      if (to) (filter.date as Record<string, string>).$lte = to;
    }
    const items = await this.checkInModel.find(filter).exec();
    return { items: mapDocs(items) };
  }

  async upsertCheckIn(habitId: string, dto: UpsertCheckInDto) {
    await this.ensureExists(habitId);
    const existing = await this.checkInModel
      .findOne({ habitId, date: dto.date })
      .exec();
    if (existing) {
      const doc = await this.checkInModel
        .findOneAndUpdate(
          { _id: existing._id },
          { status: dto.status, updatedAt: new Date(dto.updatedAt) },
          { new: true },
        )
        .exec();
      return mapDoc(doc)!;
    }
    const doc = await this.checkInModel.create({
      _id: dto.id,
      habitId,
      date: dto.date,
      status: dto.status,
      updatedAt: new Date(dto.updatedAt),
    });
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
