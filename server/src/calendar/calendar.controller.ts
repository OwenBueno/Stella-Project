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
import { CreateEventDto } from './dto/create-event.dto';
import { UpdateEventDto } from './dto/update-event.dto';
import { CalendarService } from './calendar.service';

@Controller('events')
export class CalendarController {
  constructor(private readonly calendarService: CalendarService) {}

  @Get()
  list(@Query('from') from?: string, @Query('to') to?: string) {
    return this.calendarService.list(from, to);
  }

  @Post()
  create(@Body() dto: CreateEventDto) {
    return this.calendarService.create(dto);
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.calendarService.findOne(id);
  }

  @Patch(':id')
  update(@Param('id') id: string, @Body() dto: UpdateEventDto) {
    return this.calendarService.update(id, dto);
  }

  @Delete(':id')
  @HttpCode(204)
  remove(@Param('id') id: string) {
    return this.calendarService.remove(id);
  }
}
