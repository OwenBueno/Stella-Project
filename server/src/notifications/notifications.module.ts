import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { DeviceToken, DeviceTokenSchema } from '../database/schemas/device-token.schema';
import { Task, TaskSchema } from '../database/schemas/task.schema';
import { FcmProvider } from './fcm.provider';
import { NotificationsController } from './notifications.controller';
import { NotificationsService } from './notifications.service';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Task.name, schema: TaskSchema },
      { name: DeviceToken.name, schema: DeviceTokenSchema },
    ]),
  ],
  controllers: [NotificationsController],
  providers: [FcmProvider, NotificationsService],
  exports: [NotificationsService],
})
export class NotificationsModule {}
