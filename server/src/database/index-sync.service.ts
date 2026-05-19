import { Injectable, OnModuleInit } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { CalendarEvent, CalendarEventDocument } from './schemas/calendar-event.schema';
import { DailyIntent, DailyIntentDocument } from './schemas/daily-intent.schema';
import { DeviceToken, DeviceTokenDocument } from './schemas/device-token.schema';
import { EveningReview, EveningReviewDocument } from './schemas/evening-review.schema';
import { Habit, HabitDocument } from './schemas/habit.schema';
import { HabitCheckIn, HabitCheckInDocument } from './schemas/habit-check-in.schema';
import { LifeLog, LifeLogDocument } from './schemas/life-log.schema';
import { Task, TaskDocument } from './schemas/task.schema';

@Injectable()
export class IndexSyncService implements OnModuleInit {
  constructor(
    @InjectModel(Habit.name) private readonly habitModel: Model<HabitDocument>,
    @InjectModel(HabitCheckIn.name)
    private readonly checkInModel: Model<HabitCheckInDocument>,
    @InjectModel(Task.name) private readonly taskModel: Model<TaskDocument>,
    @InjectModel(CalendarEvent.name)
    private readonly eventModel: Model<CalendarEventDocument>,
    @InjectModel(DailyIntent.name)
    private readonly dailyIntentModel: Model<DailyIntentDocument>,
    @InjectModel(EveningReview.name)
    private readonly eveningReviewModel: Model<EveningReviewDocument>,
    @InjectModel(LifeLog.name) private readonly lifeLogModel: Model<LifeLogDocument>,
    @InjectModel(DeviceToken.name)
    private readonly deviceTokenModel: Model<DeviceTokenDocument>,
  ) {}

  async onModuleInit() {
    await Promise.all([
      this.habitModel.syncIndexes(),
      this.checkInModel.syncIndexes(),
      this.taskModel.syncIndexes(),
      this.eventModel.syncIndexes(),
      this.dailyIntentModel.syncIndexes(),
      this.eveningReviewModel.syncIndexes(),
      this.lifeLogModel.syncIndexes(),
      this.deviceTokenModel.syncIndexes(),
    ]);
  }
}
