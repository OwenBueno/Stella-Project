import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
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
    scheduledFrom?: string,
    scheduledTo?: string,
  ) {
    const filter: Record<string, unknown> = { deletedAt: null };
    if (status) filter.status = status;
    if (scheduledFrom || scheduledTo) {
      filter.scheduledAt = {};
      if (scheduledFrom) {
        (filter.scheduledAt as Record<string, Date>).$gte = new Date(scheduledFrom);
      }
      if (scheduledTo) {
        (filter.scheduledAt as Record<string, Date>).$lte = new Date(scheduledTo);
      }
    }
    const items = await this.taskModel.find(filter).sort({ scheduledAt: 1 }).exec();
    return { items: mapDocs(items) };
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
    const doc = await this.taskModel.create({
      _id: dto.id,
      title: dto.title,
      notes: dto.notes ?? null,
      scheduledAt: dto.scheduledAt ? new Date(dto.scheduledAt) : null,
      durationMinutes: dto.durationMinutes ?? null,
      status: dto.status,
      priority: dto.priority ?? null,
      createdAt: new Date(dto.createdAt),
      updatedAt: new Date(dto.updatedAt),
      deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
    });
    return mapDoc(doc)!;
  }

  async update(id: string, dto: UpdateTaskDto) {
    await this.findOne(id);
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
    await this.findOne(id);
    await this.taskModel
      .updateOne({ _id: id }, { deletedAt: new Date(), updatedAt: new Date() })
      .exec();
  }
}
