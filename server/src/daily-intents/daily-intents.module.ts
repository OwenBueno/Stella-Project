import { Module } from '@nestjs/common';
import { DailyIntentsController } from './daily-intents.controller';
import { DailyIntentsService } from './daily-intents.service';

@Module({
  controllers: [DailyIntentsController],
  providers: [DailyIntentsService],
})
export class DailyIntentsModule {}
