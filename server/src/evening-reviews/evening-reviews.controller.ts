import { Body, Controller, Get, Post, Query } from '@nestjs/common';
import { CreateEveningReviewDto } from './dto/create-evening-review.dto';
import { EveningReviewsService } from './evening-reviews.service';

@Controller('evening-reviews')
export class EveningReviewsController {
  constructor(private readonly eveningReviewsService: EveningReviewsService) {}

  @Get()
  getByDate(@Query('date') date: string) {
    return this.eveningReviewsService.getByDate(date);
  }

  @Post()
  create(@Body() dto: CreateEveningReviewDto) {
    return this.eveningReviewsService.upsert(dto);
  }
}
