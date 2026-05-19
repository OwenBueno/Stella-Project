import { Module } from '@nestjs/common';
import { EveningReviewsController } from './evening-reviews.controller';
import { EveningReviewsService } from './evening-reviews.service';

@Module({
  controllers: [EveningReviewsController],
  providers: [EveningReviewsService],
})
export class EveningReviewsModule {}
