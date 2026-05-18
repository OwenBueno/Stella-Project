import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { ApiKeyGuard } from './common/guards/api-key.guard';
import { PrismaModule } from './common/prisma/prisma.module';
import { HabitsModule } from './habits/habits.module';
import { HealthController } from './health/health.controller';

@Module({
  imports: [PrismaModule, HabitsModule],
  controllers: [HealthController],
  providers: [
    {
      provide: APP_GUARD,
      useClass: ApiKeyGuard,
    },
  ],
})
export class AppModule {}
