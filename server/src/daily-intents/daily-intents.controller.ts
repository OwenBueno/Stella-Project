import { Body, Controller, Get, Post, Query } from '@nestjs/common';
import { CreateDailyIntentDto } from './dto/create-daily-intent.dto';
import { DailyIntentsService } from './daily-intents.service';

@Controller('daily-intents')
export class DailyIntentsController {
  constructor(private readonly dailyIntentsService: DailyIntentsService) {}

  @Get()
  getByDate(@Query('date') date: string) {
    return this.dailyIntentsService.getByDate(date);
  }

  @Post()
  create(@Body() dto: CreateDailyIntentDto) {
    return this.dailyIntentsService.upsert(dto);
  }
}
