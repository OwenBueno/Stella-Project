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
import { Task, TaskDocument } from '../database/schemas/task.schema';
import { CreateTaskDto } from './dto/create-task.dto';
import { UpdateTaskDto } from './dto/update-task.dto';

@Injectable()
export class TasksService {
  constructor(
    @InjectModel(Task.name) private readonly taskModel: Model<TaskDocument>,
  ) {}

  async list(
    status?: string,
    excludeStatus?: string,
    scheduledFrom?: string,
    scheduledTo?: string,
    limit?: string,
    cursor?: string,
  ) {
    const filter: Record<string, unknown> = { deletedAt: null };
    if (status) filter.status = status;
    if (excludeStatus) filter.status = { $ne: excludeStatus };
    if (scheduledFrom || scheduledTo) {
      filter.scheduledAt = {};
      if (scheduledFrom) {
        (filter.scheduledAt as Record<string, Date>).$gte = new Date(scheduledFrom);
      }
      if (scheduledTo) {
        (filter.scheduledAt as Record<string, Date>).$lte = new Date(scheduledTo);
      }
    }
    const pageLimit = clampLimit(limit);
    if (cursor || limit) {
      const docs = await this.taskModel
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
    const items = await this.taskModel.find(filter).sort({ sortOrder: 1, scheduledAt: 1 }).exec();
    return {
      items: mapDocs(items),
      nextCursor: null,
      serverTime: new Date().toISOString(),
    };
  }

  async findOne(id: string) {
    const task = await this.taskModel.findById(id).exec();
    if (!task || task.deletedAt) {
      throw new NotFoundException({
        code: 'TASK_NOT_FOUND',
        message: `Task ${id} not found`,
      });
    }
    return mapDoc(task)!;
  }

  async create(dto: CreateTaskDto) {
    const doc = await upsertByClientId(
      this.taskModel,
      dto.id,
      {
        title: dto.title,
        notes: dto.notes ?? null,
        scheduledAt: dto.scheduledAt ? new Date(dto.scheduledAt) : null,
        durationMinutes: dto.durationMinutes ?? null,
        status: dto.status,
        sortOrder: dto.sortOrder ?? 0,
        priority: dto.priority ?? null,
        updatedAt: new Date(dto.updatedAt),
        deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
      },
      new Date(dto.createdAt),
    );
    return mapDoc(doc)!;
  }

  async update(id: string, dto: UpdateTaskDto) {
    const existing = await this.taskModel.findById(id).exec();
    if (!existing) {
      throw new NotFoundException({
        code: 'TASK_NOT_FOUND',
        message: `Task ${id} not found`,
      });
    }
    const doc = await this.taskModel
      .findOneAndUpdate(
        { _id: id },
        {
          ...(dto.title !== undefined && { title: dto.title }),
          ...(dto.notes !== undefined && { notes: dto.notes }),
          ...(dto.scheduledAt !== undefined && {
            scheduledAt: dto.scheduledAt ? new Date(dto.scheduledAt) : null,
          }),
          ...(dto.durationMinutes !== undefined && {
            durationMinutes: dto.durationMinutes,
          }),
          ...(dto.status !== undefined && { status: dto.status }),
          ...(dto.sortOrder !== undefined && { sortOrder: dto.sortOrder }),
          ...(dto.priority !== undefined && { priority: dto.priority }),
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
    const existing = await this.taskModel.findById(id).exec();
    if (!existing) {
      throw new NotFoundException({
        code: 'TASK_NOT_FOUND',
        message: `Task ${id} not found`,
      });
    }
    await this.taskModel
      .updateOne({ _id: id }, { deletedAt: new Date(), updatedAt: new Date() })
      .exec();
  }
}
