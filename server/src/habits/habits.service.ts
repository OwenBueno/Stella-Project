import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../common/prisma/prisma.service';
import { CreateHabitDto } from './dto/create-habit.dto';
import { UpdateHabitDto } from './dto/update-habit.dto';

@Injectable()
export class HabitsService {
  constructor(private readonly prisma: PrismaService) {}

  async list(activeOnly = true) {
    const items = await this.prisma.habit.findMany({
      where: activeOnly ? { deletedAt: null } : undefined,
      orderBy: { sortOrder: 'asc' },
    });
    return { items };
  }

  async create(dto: CreateHabitDto) {
    return this.prisma.habit.create({
      data: {
        id: dto.id,
        name: dto.name,
        sortOrder: dto.sortOrder,
        active: dto.active ?? true,
        createdAt: new Date(dto.createdAt),
        updatedAt: new Date(dto.updatedAt),
        deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
      },
    });
  }

  async update(id: string, dto: UpdateHabitDto) {
    await this.ensureExists(id);
    return this.prisma.habit.update({
      where: { id },
      data: {
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
    });
  }

  async remove(id: string) {
    await this.ensureExists(id);
    return this.prisma.habit.update({
      where: { id },
      data: { deletedAt: new Date(), updatedAt: new Date() },
    });
  }

  private async ensureExists(id: string) {
    const habit = await this.prisma.habit.findUnique({ where: { id } });
    if (!habit || habit.deletedAt) {
      throw new NotFoundException({
        code: 'HABIT_NOT_FOUND',
        message: `Habit ${id} not found`,
      });
    }
  }
}
