import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { ApiKeyGuard } from './common/guards/api-key.guard';
import { DatabaseModule } from './database/database.module';
import { CalendarModule } from './calendar/calendar.module';
import { HabitsModule } from './habits/habits.module';
import { HealthController } from './health/health.controller';
import { DailyIntentsModule } from './daily-intents/daily-intents.module';
import { EveningReviewsModule } from './evening-reviews/evening-reviews.module';
import { FinancesModule } from './finances/finances.module';
import { SyncModule } from './sync/sync.module';
import { TasksModule } from './tasks/tasks.module';

@Module({
  imports: [
    DatabaseModule,
    HabitsModule,
    TasksModule,
    CalendarModule,
    SyncModule,
    DailyIntentsModule,
    EveningReviewsModule,
    FinancesModule,
  ],
  controllers: [HealthController],
  providers: [
    {
      provide: APP_GUARD,
      useClass: ApiKeyGuard,
    },
  ],
})
export class AppModule {}
