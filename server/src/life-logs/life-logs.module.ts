import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { LifeLog, LifeLogSchema } from '../database/schemas/life-log.schema';
import { LifeLogsController } from './life-logs.controller';
import { LifeLogsService } from './life-logs.service';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: LifeLog.name, schema: LifeLogSchema }]),
  ],
  controllers: [LifeLogsController],
  providers: [LifeLogsService],
  exports: [LifeLogsService],
})
export class LifeLogsModule {}
