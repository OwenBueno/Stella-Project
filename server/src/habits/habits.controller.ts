import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  Patch,
  Post,
  Query,
} from '@nestjs/common';
import { CreateHabitDto } from './dto/create-habit.dto';
import { UpdateHabitDto } from './dto/update-habit.dto';
import { UpsertCheckInDto } from './dto/upsert-check-in.dto';
import { HabitsService } from './habits.service';

@Controller('habits')
export class HabitsController {
  constructor(private readonly habitsService: HabitsService) {}

  @Get('check-ins')
  listCheckInsGlobal(
    @Query('from') from?: string,
    @Query('to') to?: string,
    @Query('habitId') habitId?: string,
    @Query('limit') limit?: string,
    @Query('cursor') cursor?: string,
  ) {
    return this.habitsService.listCheckInsGlobal(from, to, habitId, limit, cursor);
  }

  @Get()
  list(@Query('active') active?: string) {
    const activeOnly = active !== 'false';
    return this.habitsService.list(activeOnly);
  }

  @Post()
  create(@Body() dto: CreateHabitDto) {
    return this.habitsService.create(dto);
  }

  @Get(':habitId/check-ins')
  listCheckIns(
    @Param('habitId') habitId: string,
    @Query('from') from?: string,
    @Query('to') to?: string,
  ) {
    return this.habitsService.listCheckIns(habitId, from, to);
  }

  @Post(':habitId/check-ins')
  upsertCheckIn(
    @Param('habitId') habitId: string,
    @Body() dto: UpsertCheckInDto,
  ) {
    return this.habitsService.upsertCheckIn(habitId, dto);
  }

  @Patch(':id')
  update(@Param('id') id: string, @Body() dto: UpdateHabitDto) {
    return this.habitsService.update(id, dto);
  }

  @Delete(':id')
  @HttpCode(204)
  remove(@Param('id') id: string) {
    return this.habitsService.remove(id);
  }
}
